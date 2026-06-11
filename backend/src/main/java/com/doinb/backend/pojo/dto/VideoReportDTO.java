package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoReportDTO {
    private Integer id;
    private Integer videoId;
    private Integer reporterId;
    private String reporterNickname;
    private String reason;
    private LocalDateTime createTime;
}
