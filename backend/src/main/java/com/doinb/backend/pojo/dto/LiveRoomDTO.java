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
    /** 观众拉流地址（HLS，需流媒体服务） */
    private String playUrl;
}
