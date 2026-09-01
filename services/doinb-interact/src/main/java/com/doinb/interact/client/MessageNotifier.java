package com.doinb.interact.client;

import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.CreateNotificationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 点赞后写通知，走消息服务内部接口。消息服务挂了不影响点赞结果。 */
@Component
public class MessageNotifier {

    public static final int TYPE_LIKE_VIDEO = 1;
    public static final int TYPE_LIKE_COMMENT = 2;

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public MessageNotifier(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    /** 视频被赞。authorId 为接收者，actorId 为点赞人。 */
    public void notifyVideoLike(Integer actorId, Integer authorId, Integer videoId, String videoTitle) {
        if (actorId == null || authorId == null || videoId == null) {
            return;
        }
        CreateNotificationRequest n = new CreateNotificationRequest();
        n.setUserId(authorId);
        n.setType(TYPE_LIKE_VIDEO);
        n.setActorId(actorId);
        n.setRefId(videoId);
        n.setPreview("赞了你的视频《" + truncate(videoTitle, 20) + "》");
        n.setLinkPath("/video/" + videoId);
        post(n);
    }

    /** 评论被赞。commentAuthorId 为接收者，actorId 为点赞人。 */
    public void notifyCommentLike(Integer actorId, Integer commentAuthorId, Integer commentId, String linkPath) {
        if (actorId == null || commentAuthorId == null || commentId == null) {
            return;
        }
        CreateNotificationRequest n = new CreateNotificationRequest();
        n.setUserId(commentAuthorId);
        n.setType(TYPE_LIKE_COMMENT);
        n.setActorId(actorId);
        n.setRefId(commentId);
        n.setPreview("赞了你的评论");
        n.setLinkPath(linkPath);
        post(n);
    }

    private void post(CreateNotificationRequest request) {
        try {
            serviceClient.post(properties.getServices().getMessage(), InternalPaths.NOTIFICATIONS, request);
        } catch (RuntimeException ignored) {
            // 通知失败不能回滚赞踩结果
        }
    }

    private static String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
