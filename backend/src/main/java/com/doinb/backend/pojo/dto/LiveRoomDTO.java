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
    /** 观众拉流/播放地址（演示用占位） */
    private String playUrl;
}
