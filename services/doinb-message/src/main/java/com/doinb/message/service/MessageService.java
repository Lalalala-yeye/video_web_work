package com.doinb.message.service;

import com.doinb.common.CustomResponse;

public interface MessageService {

    CustomResponse openRoom(Integer userId, Integer peerId);

    CustomResponse getRoom(Integer userId, Integer roomId, long page, long size);

    CustomResponse send(Integer userId, Integer roomId, String content);
}
