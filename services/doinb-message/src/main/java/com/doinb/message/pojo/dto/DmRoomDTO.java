package com.doinb.message.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class DmRoomDTO {
    private Integer roomId;
    private Integer peerId;
    private String peerNickname;
    private String peerAvatar;
    private List<DmMessageDTO> messages;
}
