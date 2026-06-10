package com.doinb.backend.pojo.dto;

import lombok.Data;

/** 直播间返回结构 */
@Data
public class LiveRoomDTO {
    private Integer id;
    private String title;
    private Integer anchorId;
    private String anchorNickname;
    private String streamKey;
    private Boolean isLive;
    /** 本场开播时间 */
    private java.time.LocalDateTime sessionStart;
    /** 观众拉流地址（HLS） */
    private String playUrl;
    /** OBS 推流服务器，如 rtmp://127.0.0.1:1935/live */
    private String pushServer;
    /** 完整推流地址（ffmpeg 等可直接使用） */
    private String pushUrl;
}
