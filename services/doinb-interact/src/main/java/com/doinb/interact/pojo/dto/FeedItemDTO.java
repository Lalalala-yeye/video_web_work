package com.doinb.interact.pojo.dto;

import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.common.dto.VideoDTO;
import lombok.Data;

import java.time.LocalDateTime;

/** 订阅动态条目：视频或直播 */
@Data
public class FeedItemDTO {
    /** video 或 live */
    private String type;
    private VideoDTO video;
    private LiveRoomDTO liveRoom;
    private LocalDateTime sortTime;
}
