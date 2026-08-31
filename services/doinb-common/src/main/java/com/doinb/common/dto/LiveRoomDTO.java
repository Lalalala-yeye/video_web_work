package com.doinb.common.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LiveRoomDTO {
    private Integer id;
    private String title;
    private Integer anchorId;
    private String anchorNickname;
    private String streamKey;
    private Boolean isLive;
    private LocalDateTime sessionStart;
    private String playUrl;
}
