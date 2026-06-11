package com.doinb.backend.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.NotificationMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.NotificationDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.Notification;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    public static final int TYPE_LIKE_VIDEO = 1;
    public static final int TYPE_LIKE_COMMENT = 2;
    public static final int TYPE_MESSAGE = 3;
    public static final int TYPE_VIDEO_APPROVED = 4;

    /** 管理员/系统通知统一展示名（不暴露具体操作的管理员账号） */
    public static final String SYSTEM_ACTOR_NAME = "doinb";

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper,
                                     UserMapper userMapper,
                                     VideoMapper videoMapper,
                                     CommentMapper commentMapper) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.videoMapper = videoMapper;
        this.commentMapper = commentMapper;
    }

    @Override
    public PageResult<NotificationDTO> list(Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Notification> mpPage = new Page<>(safePage, safeSize);
        notificationMapper.selectPage(mpPage, new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreateTime));

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toDTOList(mpPage.getRecords()));
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
            return ok("已全部标为已读");
        }
        Notification n = notificationMapper.selectById(notificationId);
        if (n == null || !Objects.equals(n.getUserId(), userId)) {
            return fail(404, "通知不存在");
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, notificationId)
                .set(Notification::getIsRead, true));
        return ok("已标为已读");
    }

    @Override
    public void notifyVideoLike(Integer actorId, Integer videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null || Objects.equals(video.getAuthorId(), actorId)) {
            return;
        }
        insertNotification(video.getAuthorId(), TYPE_LIKE_VIDEO, actorId, videoId,
                "赞了你的视频《" + truncate(video.getTitle(), 20) + "》");
    }

    @Override
    public void notifyCommentLike(Integer actorId, Integer commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || Objects.equals(comment.getUserId(), actorId)) {
            return;
        }
        insertNotification(comment.getUserId(), TYPE_LIKE_COMMENT, actorId, commentId,
                "赞了你的评论");
    }

    @Override
    public void notifyMessage(Integer senderId, Integer recipientId, Integer roomId, String preview) {
        if (Objects.equals(senderId, recipientId)) {
            return;
        }
        insertNotification(recipientId, TYPE_MESSAGE, senderId, roomId, truncate(preview, 80));
    }

    @Override
    public void notifyVideoApproved(Integer adminId, Integer videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null || video.getAuthorId() == null || Objects.equals(video.getAuthorId(), adminId)) {
            return;
        }
        insertNotification(video.getAuthorId(), TYPE_VIDEO_APPROVED, adminId, videoId,
                "你的视频《" + truncate(video.getTitle(), 20) + "》已通过审核并公开发布");
    }

    private void insertNotification(Integer userId, int type, Integer actorId, Integer refId, String preview) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setActorId(actorId);
        n.setRefId(refId);
        n.setPreview(preview);
        n.setIsRead(false);
        n.setCreateTime(LocalDateTime.now());
        notificationMapper.insert(n);
    }

    private List<NotificationDTO> toDTOList(List<Notification> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Integer> actorIds = rows.stream().map(Notification::getActorId).distinct().collect(Collectors.toList());
        Map<Integer, User> userMap = userMapper.selectBatchIds(actorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<Integer, Comment> commentMap = loadCommentMap(rows);

        List<NotificationDTO> list = new ArrayList<>();
        for (Notification row : rows) {
            NotificationDTO dto = new NotificationDTO();
            dto.setId(row.getId());
            dto.setType(row.getType());
            dto.setActorId(row.getActorId());
            if (usesSystemActorName(row.getType())) {
                dto.setActorNickname(SYSTEM_ACTOR_NAME);
                dto.setActorAvatar(null);
            } else {
                User actor = userMap.get(row.getActorId());
                dto.setActorNickname(actor != null ? actor.getNickname() : "用户");
                dto.setActorAvatar(actor != null ? actor.getAvatar() : null);
            }
            dto.setRefId(row.getRefId());
            dto.setPreview(row.getPreview());
            dto.setIsRead(row.getIsRead());
            dto.setCreateTime(row.getCreateTime());
            dto.setLinkPath(buildLinkPath(row, commentMap.get(row.getRefId())));
            list.add(dto);
        }
        return list;
    }

    private Map<Integer, Comment> loadCommentMap(List<Notification> rows) {
        List<Integer> commentIds = rows.stream()
                .filter(n -> Objects.equals(n.getType(), TYPE_LIKE_COMMENT))
                .map(Notification::getRefId)
                .distinct()
                .collect(Collectors.toList());
        if (commentIds.isEmpty()) {
            return Map.of();
        }
        return commentMapper.selectBatchIds(commentIds).stream()
                .collect(Collectors.toMap(Comment::getId, c -> c));
    }

    private String buildLinkPath(Notification row, Comment comment) {
        if (Objects.equals(row.getType(), TYPE_LIKE_VIDEO)
                || Objects.equals(row.getType(), TYPE_VIDEO_APPROVED)) {
            return "/video/" + row.getRefId();
        }
        if (Objects.equals(row.getType(), TYPE_LIKE_COMMENT) && comment != null
                && Objects.equals(comment.getTargetType(), 1)) {
            return "/video/" + comment.getTargetId();
        }
        if (Objects.equals(row.getType(), TYPE_MESSAGE)) {
            return "/messages/" + row.getRefId();
        }
        return null;
    }

    /** 由平台/管理员触发的通知，前端统一显示为 doinb */
    private boolean usesSystemActorName(int type) {
        return type == TYPE_VIDEO_APPROVED;
    }

    private String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    private CustomResponse ok(String message) {
        CustomResponse resp = new CustomResponse();
        resp.setMessage(message);
        return resp;
    }

    private CustomResponse fail(int code, String message) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}
