package com.doinb.gateway.route;

import com.doinb.common.config.DoinbProperties;
import org.springframework.stereotype.Component;

@Component
public class RouteTable {

    private final DoinbProperties properties;

    public RouteTable(DoinbProperties properties) {
        this.properties = properties;
    }

    public String resolve(String path) {
        DoinbProperties.Services s = properties.getServices();
        if (path.startsWith("/video/reaction")) {
            return s.getInteract();
        }
        if (path.startsWith("/comment") || path.startsWith("/subscription")) {
            return s.getInteract();
        }
        if (path.startsWith("/admin/video") || path.startsWith("/video")) {
            return s.getVideo();
        }
        if (path.startsWith("/uploads/videos") || path.startsWith("/uploads/covers")) {
            return s.getVideo();
        }
        if (path.startsWith("/user") || path.startsWith("/admin/account") || path.startsWith("/admin/personal")) {
            return s.getUser();
        }
        if (path.startsWith("/uploads/avatars")) {
            return s.getUser();
        }
        if (path.startsWith("/live")) {
            return s.getLive();
        }
        if (path.startsWith("/notification") || path.startsWith("/message")) {
            return s.getMessage();
        }
        return null;
    }
}
