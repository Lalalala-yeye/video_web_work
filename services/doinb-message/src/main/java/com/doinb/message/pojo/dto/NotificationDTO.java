package com.doinb.message.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Integer id;
    private Integer type;
    private Integer actorId;
    private String actorNickname;
    private String actorAvatar;
    private Integer refId;
    private String preview;
    private Boolean isRead;
    private LocalDateTime createTime;
    private String linkPath;
}
