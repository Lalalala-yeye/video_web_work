package com.doinb.common.web;

import com.doinb.common.GatewayHeaders;
import jakarta.servlet.http.HttpServletRequest;

/** 下游服务从网关注入的头读取当前用户。 */
public final class GatewayUser {

    private GatewayUser() {
    }

    public static Integer userId(HttpServletRequest request) {
        String raw = request.getHeader(GatewayHeaders.USER_ID);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Integer.valueOf(raw);
    }

    public static Integer requireUserId(HttpServletRequest request) {
        Integer id = userId(request);
        if (id == null) {
            throw new IllegalStateException("未登录");
        }
        return id;
    }

    public static String role(HttpServletRequest request) {
        String role = request.getHeader(GatewayHeaders.USER_ROLE);
        return role == null ? "" : role;
    }

    public static boolean isAdmin(HttpServletRequest request) {
        return GatewayHeaders.ROLE_ADMIN.equalsIgnoreCase(role(request));
    }
}
