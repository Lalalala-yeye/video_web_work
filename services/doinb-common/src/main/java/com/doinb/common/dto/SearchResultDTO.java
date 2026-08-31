package com.doinb.common.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResultDTO {
    private List<VideoDTO> videos = new ArrayList<>();
    private List<LiveRoomDTO> liveRooms = new ArrayList<>();
    private List<UserDTO> users = new ArrayList<>();
}
