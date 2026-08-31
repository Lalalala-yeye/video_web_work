package com.doinb.message.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.web.GatewayUser;
import com.doinb.message.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notification/list")
    public CustomResponse list(HttpServletRequest request,
                               @RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "20") long size) {
        return CustomResponse.ok(notificationService.list(GatewayUser.requireUserId(request), page, size));
    }

    @GetMapping("/notification/unread-count")
    public CustomResponse unreadCount(HttpServletRequest request) {
        return CustomResponse.ok(Map.of("count",
                notificationService.countUnread(GatewayUser.requireUserId(request))));
    }

    @PostMapping("/notification/read")
    public CustomResponse markRead(HttpServletRequest request,
                                   @RequestParam(value = "id", required = false) Integer id) {
        return notificationService.markRead(GatewayUser.requireUserId(request), id);
    }
}
