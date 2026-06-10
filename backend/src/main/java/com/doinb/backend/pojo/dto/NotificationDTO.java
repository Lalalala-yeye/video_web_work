package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Integer id;
    /** 1=视频被赞 2=评论被赞 3=私信 */
    private Integer type;
    private Integer actorId;
    private String actorNickname;
    private String actorAvatar;
    private Integer refId;
    private String preview;
    private Boolean isRead;
    private LocalDateTime createTime;
    /** 前端跳转路径 */
    private String linkPath;
}
