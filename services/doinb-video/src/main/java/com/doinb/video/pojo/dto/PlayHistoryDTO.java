package com.doinb.video.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PlayHistoryDTO {
    private Integer videoId;
    private String title;
    private String coverUrl;
    private Integer progress;
    private LocalDateTime updateTime;
}
