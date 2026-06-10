package com.doinb.backend.service.message;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.DmRoomDTO;

public interface MessageService {

    CustomResponse openRoom(Integer userId, Integer peerId);

    CustomResponse getRoom(Integer userId, Integer roomId, long page, long size);

    CustomResponse send(Integer userId, Integer roomId, String content);
}
