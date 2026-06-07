package com.doinb.backend.service.live;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.LiveRoomDTO;
import com.doinb.backend.pojo.dto.PageResult;

/** 直播间业务 */
public interface LiveRoomService {

    PageResult<LiveRoomDTO> list(long page, long size);

    CustomResponse getOne(Integer id);

    CustomResponse create(Integer userId, Integer role, String title);

    CustomResponse startLive(Integer userId, Integer role, Integer roomId);

    CustomResponse stopLive(Integer userId, Integer role, Integer roomId);
}
