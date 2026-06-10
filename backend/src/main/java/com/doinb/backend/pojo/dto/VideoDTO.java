package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 返回给前端的视频信息（含作者昵称，便于列表展示）
 */
@Data
public class VideoDTO {
    private Integer id;
    private String title;
    private String description;
    private Integer authorId;
    private String authorNickname;
    private String authorAvatar;
    private String coverUrl;
    private String videoUrl;
    /** 0审核中 1已发布 2已下架 */
    private Integer status;
    private LocalDateTime createTime;
    private ReactionSummaryDTO reactions;
}
