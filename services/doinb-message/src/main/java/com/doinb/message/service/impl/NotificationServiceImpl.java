package com.doinb.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.CreateNotificationRequest;
import com.doinb.common.dto.UserDTO;
import com.doinb.message.client.UserDirectoryClient;
import com.doinb.message.mapper.NotificationMapper;
import com.doinb.message.pojo.dto.NotificationDTO;
import com.doinb.message.pojo.entity.Notification;
import com.doinb.message.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class NotificationServiceImpl implements NotificationService {

    public static final int TYPE_LIKE_VIDEO = 1;
    public static final int TYPE_LIKE_COMMENT = 2;
    public static final int TYPE_MESSAGE = 3;
    public static final int TYPE_VIDEO_APPROVED = 4;
    public static final String SYSTEM_ACTOR_NAME = "doinb";

    private final NotificationMapper notificationMapper;
    private final UserDirectoryClient userDirectoryClient;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                   UserDirectoryClient userDirectoryClient) {
        this.notificationMapper = notificationMapper;
        this.userDirectoryClient = userDirectoryClient;
    }

    @Override
    public PageResult<NotificationDTO> list(Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);
        Page<Notification> resultPage = new Page<>(safePage, safeSize);
        notificationMapper.selectPage(resultPage, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime));

        List<Notification> rows = resultPage.getRecords();
        Map<Integer, UserDTO> actors = userDirectoryClient.findByIds(rows.stream()
                .filter(row -> !usesSystemActorName(row.getType()))
                .map(Notification::getActorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return new PageResult<>(resultPage.getTotal(), safePage, safeSize, toDTOList(rows, actors));
    }

    @Override
    public long countUnread(Integer userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, false));
    }

    @Override
    public CustomResponse markRead(Integer userId, Integer notificationId) {
        if (notificationId == null) {
            notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                    .eq(Notification::getUserId, userId)
                    .eq(Notification::getIsRead, false)
                    .set(Notification::getIsRead, true));
            return CustomResponse.ok("已全部标为已读", null);
        }
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !Objects.equals(notification.getUserId(), userId)) {
            return CustomResponse.fail(404, "通知不存在");
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .set(Notification::getIsRead, true));
        return CustomResponse.ok("已标为已读", null);
    }

    @Override
    public CustomResponse create(CreateNotificationRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return CustomResponse.fail(400, validationError);
        }
        if (Objects.equals(request.getUserId(), request.getActorId())) {
            return CustomResponse.ok("已忽略本人操作", null);
        }
        Notification notification = insertNotification(
                request.getUserId(), request.getType(), request.getActorId(), request.getRefId(),
                request.getPreview(), request.getLinkPath());
        return CustomResponse.ok("通知创建成功", Map.of("id", notification.getId()));
    }

    @Override
    public void notifyMessage(Integer senderId, Integer recipientId, Integer roomId, String preview) {
        if (Objects.equals(senderId, recipientId)) {
            return;
        }
        insertNotification(recipientId, TYPE_MESSAGE, senderId, roomId, preview, "/messages/" + roomId);
    }

    private Notification insertNotification(Integer userId, int type, Integer actorId, Integer refId,
                                            String preview, String linkPath) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setActorId(actorId);
        notification.setRefId(refId);
        notification.setPreview(truncate(preview, 255));
        notification.setLinkPath(resolveLinkPath(type, refId, linkPath));
        notification.setIsRead(false);
        notification.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(notification);
        return notification;
    }

    private List<NotificationDTO> toDTOList(List<Notification> rows, Map<Integer, UserDTO> actors) {
        List<NotificationDTO> result = new ArrayList<>();
        for (Notification row : rows) {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(row.getId());
            dto.setType(row.getType());
            dto.setActorId(row.getActorId());
            if (usesSystemActorName(row.getType())) {
                dto.setActorNickname(SYSTEM_ACTOR_NAME);
                dto.setActorAvatar(null);
            } else {
                UserDTO actor = actors.get(row.getActorId());
                dto.setActorNickname(actor != null && StringUtils.hasText(actor.getNickname())
                        ? actor.getNickname() : "用户");
                dto.setActorAvatar(actor != null ? actor.getAvatar() : null);
            }
            dto.setRefId(row.getRefId());
            dto.setPreview(row.getPreview());
            dto.setIsRead(row.getIsRead());
            dto.setCreateTime(row.getCreateTime());
            dto.setLinkPath(resolveLinkPath(row.getType(), row.getRefId(), row.getLinkPath()));
            result.add(dto);
        }
        return result;
    }

    private static String validate(CreateNotificationRequest request) {
        if (request == null) {
            return "通知内容不能为空";
        }
        if (request.getUserId() == null) {
            return "接收者 id 不能为空";
        }
        if (request.getActorId() == null) {
            return "触发者 id 不能为空";
        }
        if (request.getType() == null || request.getType() < TYPE_LIKE_VIDEO
                || request.getType() > TYPE_VIDEO_APPROVED) {
            return "通知类型无效";
        }
        return null;
    }

    private static boolean usesSystemActorName(Integer type) {
        return Objects.equals(type, TYPE_VIDEO_APPROVED);
    }

    private static String resolveLinkPath(Integer type, Integer refId, String supplied) {
        if (StringUtils.hasText(supplied)) {
            return supplied.trim();
        }
        if (refId == null) {
            return null;
        }
        if (Objects.equals(type, TYPE_MESSAGE)) {
            return "/messages/" + refId;
        }
        if (Objects.equals(type, TYPE_LIKE_VIDEO)
                || Objects.equals(type, TYPE_LIKE_COMMENT)
                || Objects.equals(type, TYPE_VIDEO_APPROVED)) {
            return "/video/" + refId;
        }
        return null;
    }

    private static String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
