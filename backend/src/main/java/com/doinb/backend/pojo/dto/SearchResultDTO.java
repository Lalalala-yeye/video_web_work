package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.util.List;

/** 搜索结果 */
@Data
public class SearchResultDTO {
    private List<VideoDTO> videos;
    private List<LiveRoomDTO> liveRooms;
    private List<UserDTO> users;
}
