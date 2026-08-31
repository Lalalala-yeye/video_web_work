package com.doinb.gateway.filter;

import com.doinb.common.GatewayHeaders;
import com.doinb.common.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 网关验 JWT。公开路径可不带 token；带了非法 token 一律 403。
 * 合法用户写入 request attribute，由转发过滤器注入下游头。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "doinb.userId";
    public static final String ATTR_ROLE = "doinb.role";

    private static final Set<String> PUBLIC_EXACT = Set.of(
            "/health",
            "/search",
            "/user/account/register",
            "/user/account/login",
            "/admin/account/login",
            "/user/info/get-one",
            "/user/profile/showcase",
            "/video/reaction/summary",
            "/video/list",
            "/video/getone",
            "/live/list",
            "/live/getone",
            "/comment/list"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String token = resolveToken(request);

        if (StringUtils.hasText(token)) {
            if (!jwtUtil.isTokenValid(token)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            request.setAttribute(ATTR_USER_ID, jwtUtil.getUserIdFromToken(token));
            request.setAttribute(ATTR_ROLE, jwtUtil.getRoleFromToken(token));
        } else if (!isPublic(path)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        if (PUBLIC_EXACT.contains(path) || path.startsWith("/uploads/")) {
            return true;
        }
        return false;
    }

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
