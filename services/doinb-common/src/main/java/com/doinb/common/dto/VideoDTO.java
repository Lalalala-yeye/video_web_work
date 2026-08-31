package com.doinb.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

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
    /** 0待审核 1已发布 2举报待复审 3仅自己可见 */
    private Integer status;
    private Integer reportCount;
    private LocalDateTime createTime;
}
