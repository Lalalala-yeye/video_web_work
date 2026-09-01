package com.doinb.gateway.filter;

import com.doinb.common.GatewayHeaders;
import com.doinb.common.InternalPaths;
import com.doinb.common.config.DoinbProperties;
import com.doinb.gateway.route.RouteTable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Enumeration;
import java.util.Set;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class GatewayProxyFilter extends OncePerRequestFilter {

    private static final Set<String> HOP_BY_HOP = Set.of(
            "host", "connection", "keep-alive", "transfer-encoding", "te",
            "trailer", "proxy-authorization", "proxy-authenticate", "upgrade",
            "content-length", "authorization"
    );

    private final RouteTable routeTable;
    private final DoinbProperties properties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public GatewayProxyFilter(RouteTable routeTable, DoinbProperties properties) {
        this.routeTable = routeTable;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("/health".equals(path) || "/ready".equals(path) || "/version".equals(path) || "/search".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (path.startsWith(InternalPaths.PREFIX)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String target = routeTable.resolve(path);
        if (target == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String query = request.getQueryString();
        String uri = target + path + (query == null || query.isBlank() ? "" : "?" + query);
        Duration timeout = path.startsWith("/video/upload") ? Duration.ofMinutes(10) : Duration.ofSeconds(30);

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri))
                    .timeout(timeout)
                    .method(request.getMethod(), bodyPublisher(request));

            Enumeration<String> names = request.getHeaderNames();
            while (names.hasMoreElements()) {
                String name = names.nextElement();
                if (HOP_BY_HOP.contains(name.toLowerCase())
                        || name.equalsIgnoreCase(GatewayHeaders.USER_ID)
                        || name.equalsIgnoreCase(GatewayHeaders.USER_ROLE)
                        || name.equalsIgnoreCase(GatewayHeaders.INTERNAL_TOKEN)) {
                    continue;
                }
                Enumeration<String> values = request.getHeaders(name);
                while (values.hasMoreElements()) {
                    builder.header(name, values.nextElement());
                }
            }

            Object userId = request.getAttribute(JwtAuthFilter.ATTR_USER_ID);
            Object role = request.getAttribute(JwtAuthFilter.ATTR_ROLE);
            if (userId != null) {
                builder.header(GatewayHeaders.USER_ID, String.valueOf(userId));
            }
            if (role != null && !String.valueOf(role).isBlank()) {
                builder.header(GatewayHeaders.USER_ROLE, String.valueOf(role));
            }
            builder.header(GatewayHeaders.INTERNAL_TOKEN, properties.getInternalToken());

            HttpResponse<InputStream> upstream = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            response.setStatus(upstream.statusCode());
            upstream.headers().map().forEach((name, values) -> {
                if (!HOP_BY_HOP.contains(name.toLowerCase())) {
                    values.forEach(value -> response.addHeader(name, value));
                }
            });
            try (InputStream in = upstream.body()) {
                in.transferTo(response.getOutputStream());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    private HttpRequest.BodyPublisher bodyPublisher(HttpServletRequest request) throws IOException {
        if ("GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod())) {
            return HttpRequest.BodyPublishers.noBody();
        }
        if (request.getContentLength() == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofInputStream(() -> {
            try {
                return request.getInputStream();
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        });
    }
}
