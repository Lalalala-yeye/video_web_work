package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 评论返回结构 */
@Data
public class CommentDTO {
    private Integer id;
    private Integer userId;
    private String userNickname;
    private String userAvatar;
    private Integer targetId;
    private Integer targetType;
    private String content;
    private LocalDateTime createTime;
}
