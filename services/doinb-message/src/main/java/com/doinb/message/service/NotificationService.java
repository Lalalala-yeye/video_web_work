package com.doinb.message.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.CreateNotificationRequest;
import com.doinb.message.pojo.dto.NotificationDTO;

public interface NotificationService {

    PageResult<NotificationDTO> list(Integer userId, long page, long size);

    long countUnread(Integer userId);

    CustomResponse markRead(Integer userId, Integer notificationId);

    CustomResponse create(CreateNotificationRequest request);

    void notifyMessage(Integer senderId, Integer recipientId, Integer roomId, String preview);
}
