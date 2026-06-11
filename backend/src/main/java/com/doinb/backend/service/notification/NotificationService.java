package com.doinb.backend.service.notification;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.NotificationDTO;
import com.doinb.backend.pojo.dto.PageResult;

public interface NotificationService {

    PageResult<NotificationDTO> list(Integer userId, long page, long size);

    long countUnread(Integer userId);

    CustomResponse markRead(Integer userId, Integer notificationId);

    void notifyVideoLike(Integer actorId, Integer videoId);

    void notifyCommentLike(Integer actorId, Integer commentId);

    void notifyMessage(Integer senderId, Integer recipientId, Integer roomId, String preview);

    /** 视频审核通过，通知发布者 */
    void notifyVideoApproved(Integer adminId, Integer videoId);
}
