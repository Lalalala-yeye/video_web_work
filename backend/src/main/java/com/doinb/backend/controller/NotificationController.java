package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.NotificationDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.service.notification.NotificationService;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUser currentUser;

    public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
        this.notificationService = notificationService;
        this.currentUser = currentUser;
    }

    @GetMapping("/notification/list")
    public CustomResponse list(@RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "20") long size) {
        Integer userId = currentUser.getUserId();
        PageResult<NotificationDTO> result = notificationService.list(userId, page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    @GetMapping("/notification/unread-count")
    public CustomResponse unreadCount() {
        Integer userId = currentUser.getUserId();
        Map<String, Object> data = new HashMap<>();
        data.put("count", notificationService.countUnread(userId));
        CustomResponse resp = new CustomResponse();
        resp.setData(data);
        return resp;
    }

    @PostMapping("/notification/read")
    public CustomResponse markRead(@RequestParam(value = "id", required = false) Integer id) {
        return notificationService.markRead(currentUser.getUserId(), id);
    }
}
