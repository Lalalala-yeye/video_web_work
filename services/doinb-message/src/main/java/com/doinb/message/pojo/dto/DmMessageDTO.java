package com.doinb.message.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DmMessageDTO {
    private Integer id;
    private Integer roomId;
    private Integer senderId;
    private String senderNickname;
    private String senderAvatar;
    private String content;
    private LocalDateTime createTime;
    private Boolean mine;
}
