package com.doinb.backend.pojo.dto;

import lombok.Data;

/** 直播间返回结构 */
@Data
public class LiveRoomDTO {
    private Integer id;
    private String title;
    private Integer anchorId;
    private String anchorNickname;
    /** 仅主播本人「我的直播」等接口返回 */
    private String streamKey;
    private Boolean isLive;
    /** 本场开播时间 */
    private java.time.LocalDateTime sessionStart;
    /** 观众拉流相对路径（如 /live/xxx.m3u8），前端按当前站点代理播放 */
    private String playUrl;
}
