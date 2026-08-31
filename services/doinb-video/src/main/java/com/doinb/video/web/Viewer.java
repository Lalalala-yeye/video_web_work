package com.doinb.video.web;

import com.doinb.common.web.GatewayUser;
import jakarta.servlet.http.HttpServletRequest;

public final class Viewer {

    private Viewer() {
    }

    public static Integer userId(HttpServletRequest request) {
        return GatewayUser.userId(request);
    }

    public static Integer requireUserId(HttpServletRequest request) {
        return GatewayUser.requireUserId(request);
    }

    /** 网关只注入 user/admin 字符串，管理接口用整数 2 与单体一致。 */
    public static Integer role(HttpServletRequest request) {
        if (GatewayUser.userId(request) == null) {
            return null;
        }
        return GatewayUser.isAdmin(request) ? 2 : 1;
    }

    public static Integer requireRole(HttpServletRequest request) {
        Integer role = role(request);
        return role == null ? 1 : role;
    }
}
