package com.doinb.gateway.filter;

import com.doinb.common.GatewayHeaders;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.jwt.JwtUtil;
import com.doinb.gateway.route.RouteTable;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GatewayFilterContractTest {

    private HttpServer server;
    private MockMvc mockMvc;
    private JwtUtil jwtUtil;
    private final AtomicReference<String> capturedUserId = new AtomicReference<>();
    private final AtomicReference<String> capturedRole = new AtomicReference<>();
    private final AtomicReference<String> capturedInternal = new AtomicReference<>();
    private final AtomicReference<String> capturedAuth = new AtomicReference<>();
    private final AtomicReference<String> capturedPath = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            capturedPath.set(exchange.getRequestURI().getPath());
            capturedUserId.set(exchange.getRequestHeaders().getFirst(GatewayHeaders.USER_ID));
            capturedRole.set(exchange.getRequestHeaders().getFirst(GatewayHeaders.USER_ROLE));
            capturedInternal.set(exchange.getRequestHeaders().getFirst(GatewayHeaders.INTERNAL_TOKEN));
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "{\"code\":200,\"message\":\"OK\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "doinb-jwt-local-secret-key-at-least-32-chars-ok");
        ReflectionTestUtils.setField(jwtUtil, "expireMillis", 86400000L);

        DoinbProperties properties = new DoinbProperties();
        properties.setRole("gateway");
        properties.setInternalToken("test-internal-token");
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        properties.getServices().setUser(base);
        properties.getServices().setVideo(base);
        properties.getServices().setLive(base);
        properties.getServices().setInteract(base);
        properties.getServices().setMessage(base);

        mockMvc = standaloneSetup(new LocalOnlyController())
                .addFilters(new JwtAuthFilter(jwtUtil), new GatewayProxyFilter(new RouteTable(properties), properties))
                .build();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void publicList_withoutToken_isForwardedWithInternalToken() throws Exception {
        mockMvc.perform(get("/video/list"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"code\":200,\"message\":\"OK\"}"));
        assertEquals("/video/list", capturedPath.get());
        assertNull(capturedUserId.get());
        assertEquals("test-internal-token", capturedInternal.get());
        assertNull(capturedAuth.get());
    }

    @Test
    void protectedPath_withoutToken_returns403() throws Exception {
        mockMvc.perform(get("/video/history/list")).andExpect(status().isForbidden());
        assertNull(capturedPath.get());
    }

    @Test
    void protectedPath_withValidToken_injectsUserHeaders() throws Exception {
        String token = jwtUtil.createToken(10, "admin");
        mockMvc.perform(get("/admin/video/pending").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertEquals("/admin/video/pending", capturedPath.get());
        assertEquals("10", capturedUserId.get());
        assertEquals("admin", capturedRole.get());
        assertEquals("test-internal-token", capturedInternal.get());
        assertNull(capturedAuth.get());
    }

    @Test
    void invalidToken_onPublicPath_returns403() throws Exception {
        mockMvc.perform(get("/video/list").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isForbidden());
    }

    @Test
    void browserInternal_isBlocked() throws Exception {
        mockMvc.perform(get("/internal/videos/1")).andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/notifications")).andExpect(status().isForbidden());
        String token = jwtUtil.createToken(10, "user");
        mockMvc.perform(get("/internal/videos/1").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        assertNull(capturedPath.get());
    }

    @Test
    void unknownPath_withToken_returns404() throws Exception {
        String token = jwtUtil.createToken(10, "user");
        mockMvc.perform(get("/no-such").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchAndHealth_stayOnGateway() throws Exception {
        mockMvc.perform(get("/search").param("keyword", "x")).andExpect(status().isOk());
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        mockMvc.perform(get("/ready")).andExpect(status().isOk());
        mockMvc.perform(get("/version")).andExpect(status().isOk());
        assertNull(capturedPath.get());
    }

    @RestController
    static class LocalOnlyController {
        @GetMapping("/search")
        String search() {
            return "search";
        }

        @GetMapping("/health")
        String health() {
            return "ok";
        }

        @GetMapping("/ready")
        String ready() {
            return "ready";
        }

        @GetMapping("/version")
        String version() {
            return "dev";
        }
    }
}
