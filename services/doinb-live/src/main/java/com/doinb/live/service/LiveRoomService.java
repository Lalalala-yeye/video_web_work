package com.doinb.live.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.LiveRoomDTO;

public interface LiveRoomService {

    PageResult<LiveRoomDTO> list(long page, long size);

    CustomResponse getOne(Integer id, Integer viewerUserId, boolean viewerIsAdmin);

    CustomResponse create(Integer userId, String title);

    CustomResponse startLive(Integer userId, boolean isAdmin, Integer roomId);

    CustomResponse stopLive(Integer userId, boolean isAdmin, Integer roomId);

    PageResult<LiveRoomDTO> listMyRooms(Integer userId, long page, long size);
}
