package com.doinb.common.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResultDTO {
    private List<VideoDTO> videos = new ArrayList<>();
    private List<LiveRoomDTO> liveRooms = new ArrayList<>();
    private List<UserDTO> users = new ArrayList<>();
    /** 某一路下游超时/失败时的降级说明；全成功则为空。 */
    private List<String> notices = new ArrayList<>();
}
