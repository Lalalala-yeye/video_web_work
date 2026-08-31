package com.doinb.video.client;

import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.CreateNotificationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** 审核通过后通知发布者。消息服务挂了不影响审核结果。 */
@Component
public class MessageNotifier {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public MessageNotifier(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    public void notifyVideoApproved(Integer adminId, Integer authorId, Integer videoId, String title) {
        if (adminId == null || authorId == null || videoId == null) {
            return;
        }
        CreateNotificationRequest n = new CreateNotificationRequest();
        n.setUserId(authorId);
        n.setType(4);
        n.setActorId(adminId);
        n.setRefId(videoId);
        n.setPreview("你的视频《" + truncate(title, 20) + "》已通过审核并公开发布");
        n.setLinkPath("/video/" + videoId);
        try {
            serviceClient.post(properties.getServices().getMessage(), InternalPaths.NOTIFICATIONS, n);
        } catch (RuntimeException ignored) {
            // 通知失败不能回滚审核结果
        }
    }

    private static String truncate(String text, int max) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }
}
