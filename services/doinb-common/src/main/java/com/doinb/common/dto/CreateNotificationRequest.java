package com.doinb.common.dto;

import lombok.Data;

/**
 * 其它服务调用消息服务创建通知。
 * type：1=视频被赞 2=评论被赞 3=私信 4=审核通过
 */
@Data
public class CreateNotificationRequest {
    private Integer userId;
    private Integer type;
    private Integer actorId;
    private Integer refId;
    private String preview;
    /** 可选；有则消息服务直接保存，不必再回问评论/视频 */
    private String linkPath;
}
