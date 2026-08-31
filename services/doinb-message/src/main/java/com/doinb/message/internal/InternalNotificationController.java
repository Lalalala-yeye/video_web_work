package com.doinb.message.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.dto.CreateNotificationRequest;
import com.doinb.message.service.NotificationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping(InternalPaths.NOTIFICATIONS)
    public CustomResponse create(@RequestBody CreateNotificationRequest request) {
        return notificationService.create(request);
    }
}
