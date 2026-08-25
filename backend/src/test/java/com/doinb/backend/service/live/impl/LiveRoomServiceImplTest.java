package com.doinb.backend.service.live.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.config.LiveStreamHelper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 L010 / L020 / L021 / L022 */
class LiveRoomServiceImplTest {

    private LiveRoomMapper liveRoomMapper;
    private UserMapper userMapper;
    private LiveRoomServiceImpl service;

    @BeforeEach
    void setUp() {
        liveRoomMapper = mock(LiveRoomMapper.class);
        userMapper = mock(UserMapper.class);
        service = new LiveRoomServiceImpl(liveRoomMapper, userMapper, mock(LiveStreamHelper.class));
    }

    @Test
    void create_whenTitleBlank_returns400() {
        CustomResponse resp = service.create(10, 1, "  ");

        assertEquals(400, resp.getCode());
        assertEquals("标题不能为空", resp.getMessage());
        verify(liveRoomMapper, never()).insert(any(LiveRoom.class));
    }

    @Test
    void create_whenValid_insertsRoom() {
        when(userMapper.selectById(10)).thenReturn(new User());

        CustomResponse resp = service.create(10, 1, "测试直播间");

        assertEquals(200, resp.getCode());
        assertEquals("创建成功，请点击开播", resp.getMessage());
        verify(liveRoomMapper).insert(any(LiveRoom.class));
    }

    @Test
    void startLive_whenMissing_returns404() {
        when(liveRoomMapper.selectById(3)).thenReturn(null);

        CustomResponse resp = service.startLive(10, 1, 3);

        assertEquals(404, resp.getCode());
        assertEquals("直播间不存在", resp.getMessage());
    }

    @Test
    void startLive_whenNotOwner_returns403() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setAnchorId(99);
        room.setIsLive(false);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.startLive(10, 1, 3);

        assertEquals(403, resp.getCode());
        assertEquals("无权开播", resp.getMessage());
    }

    @Test
    void startLive_whenAlreadyLive_returns400() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setAnchorId(10);
        room.setIsLive(true);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.startLive(10, 1, 3);

        assertEquals(400, resp.getCode());
        assertEquals("已在直播中", resp.getMessage());
    }

    @Test
    void startLive_whenOwner_returns200() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setAnchorId(10);
        room.setIsLive(false);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.startLive(10, 1, 3);

        assertEquals(200, resp.getCode());
        assertEquals("开播成功", resp.getMessage());
        verify(liveRoomMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void stopLive_whenNotLive_returns400() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setAnchorId(10);
        room.setIsLive(false);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.stopLive(10, 1, 3);

        assertEquals(400, resp.getCode());
        assertEquals("当前未在直播", resp.getMessage());
    }

    @Test
    void getOne_whenMissing_returns404() {
        when(liveRoomMapper.selectById(3)).thenReturn(null);

        CustomResponse resp = service.getOne(3, 10, 1);

        assertEquals(404, resp.getCode());
        assertEquals("直播间不存在", resp.getMessage());
    }

    @Test
    void getOne_whenNotLiveAndNotOwner_returns404() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setAnchorId(99);
        room.setIsLive(false);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.getOne(3, 10, 1);

        assertEquals(404, resp.getCode());
        assertEquals("直播间未开播或已结束", resp.getMessage());
    }

    @Test
    void stopLive_whenOwner_returns200() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setAnchorId(10);
        room.setIsLive(true);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.stopLive(10, 1, 3);

        assertEquals(200, resp.getCode());
        assertEquals("停播成功", resp.getMessage());
        verify(liveRoomMapper).update(isNull(), any(Wrapper.class));
    }
}
