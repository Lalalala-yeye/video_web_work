package com.doinb.common.web;

import com.doinb.common.GatewayHeaders;
import com.doinb.common.InternalPaths;
import com.doinb.common.config.DoinbProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 下游服务不验 JWT：/internal 校验内部令牌，其余受保护路径要求网关带来的 X-User-Id。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "doinb.role", havingValue = "service")
public class DownstreamAuthFilter extends OncePerRequestFilter {

    private final DoinbProperties properties;

    public DownstreamAuthFilter(DoinbProperties properties) {
        this.properties = properties;
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
        if (path.startsWith(InternalPaths.PREFIX)) {
            String token = request.getHeader(GatewayHeaders.INTERNAL_TOKEN);
            if (!properties.getInternalToken().equals(token)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        if (isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!StringUtils.hasText(request.getHeader(GatewayHeaders.USER_ID))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        for (String prefix : properties.getPublicPathPrefixes()) {
            if (path.equals(prefix) || path.startsWith(prefix.endsWith("/") ? prefix : prefix + "/")) {
                return true;
            }
        }
        return false;
    }
}
