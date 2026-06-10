package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 播放历史条目：视频摘要 + 看到第几秒
 */
@Data
public class PlayHistoryDTO {
    private Integer videoId;
    private String title;
    private String coverUrl;
    private Integer progress;
    private LocalDateTime updateTime;
}
