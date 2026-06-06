package com.doinb.backend.config.filter;

import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.impl.UserDetailsImpl;
import com.doinb.backend.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：每个 HTTP 请求都会先经过这里。
 * <p>
 * 若请求头带了合法的 token，就把用户信息放进 SecurityContext，
 * 后面的 Controller / Service 就能通过 CurrentUser 知道是谁在访问。
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public JwtAuthenticationTokenFilter(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        // 没有 token：直接放行，是否允许访问由 SecurityConfig 里的 permitAll / authenticated 决定
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 有 token 但无效或过期
        if (!jwtUtil.isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setHeader("message", "not login");
            return;
        }

        // 从 token 解析出 userId，再查数据库拿到最新用户信息
        Integer userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setHeader("message", "not login");
            return;
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setHeader("message", "not login");
            return;
        }

        // 管理员 token 只能给管理员用（防止普通 user token 误访问 admin 接口时仍能通过 Filter）
        String tokenRole = jwtUtil.getRoleFromToken(token);
        if ("admin".equals(tokenRole) && (user.getRole() == null || user.getRole() != 2)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setHeader("message", "not login");
            return;
        }

        // 把登录用户写入 Spring Security 上下文
        UserDetailsImpl loginUser = new UserDetailsImpl(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头 Authorization 里取出 token。
     * 支持两种格式：Bearer xxx  或直接 xxx（与 README 约定兼容）
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (!StringUtils.hasText(header)) {
            return null;
        }
        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return header.trim();
    }
}
