package com.doinb.backend.service.message.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.DmMessageMapper;
import com.doinb.backend.mapper.DmRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.DmRoomDTO;
import com.doinb.backend.pojo.entity.DmMessage;
import com.doinb.backend.pojo.entity.DmRoom;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 M000 / M010 / M020 */
class MessageServiceImplTest {

    private DmRoomMapper dmRoomMapper;
    private DmMessageMapper dmMessageMapper;
    private UserMapper userMapper;
    private NotificationService notificationService;
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        dmRoomMapper = mock(DmRoomMapper.class);
        dmMessageMapper = mock(DmMessageMapper.class);
        userMapper = mock(UserMapper.class);
        notificationService = mock(NotificationService.class);
        service = new MessageServiceImpl(dmRoomMapper, dmMessageMapper, userMapper, notificationService);
    }

    @Test
    void openRoom_whenSelf_returns400() {
        CustomResponse resp = service.openRoom(10, 10);

        assertEquals(400, resp.getCode());
        assertEquals("不能给自己发私信", resp.getMessage());
        verify(dmRoomMapper, never()).insert(any(DmRoom.class));
    }

    @Test
    void openRoom_whenPeerMissing_returns404() {
        when(userMapper.selectById(11)).thenReturn(null);

        CustomResponse resp = service.openRoom(10, 11);

        assertEquals(404, resp.getCode());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void send_whenContentBlank_returns400() {
        CustomResponse resp = service.send(10, 2, "  ");

        assertEquals(400, resp.getCode());
        assertEquals("消息不能为空", resp.getMessage());
        verify(dmMessageMapper, never()).insert(any(DmMessage.class));
    }

    @Test
    void send_whenRoomMissing_returns404() {
        when(dmRoomMapper.selectById(2)).thenReturn(null);

        CustomResponse resp = service.send(10, 2, "你好，测试私信");

        assertEquals(404, resp.getCode());
        assertEquals("会话不存在", resp.getMessage());
    }

    @Test
    void send_whenMember_returns200AndNotifies() {
        DmRoom room = new DmRoom();
        room.setId(2);
        room.setUserA(10);
        room.setUserB(11);
        when(dmRoomMapper.selectById(2)).thenReturn(room);
        when(userMapper.selectById(10)).thenReturn(new User());

        CustomResponse resp = service.send(10, 2, "你好，测试私信");

        assertEquals(200, resp.getCode());
        assertEquals("发送成功", resp.getMessage());
        verify(dmMessageMapper).insert(any(DmMessage.class));
        verify(notificationService).notifyMessage(10, 11, 2, "你好，测试私信");
    }

    @Test
    void openRoom_whenPeerExists_returns200() {
        User peer = new User();
        peer.setId(11);
        peer.setNickname("用户_admin_test");
        when(userMapper.selectById(11)).thenReturn(peer);
        DmRoom room = new DmRoom();
        room.setId(2);
        room.setUserA(10);
        room.setUserB(11);
        when(dmRoomMapper.selectOne(any(Wrapper.class))).thenReturn(room);
        when(dmMessageMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<DmMessage> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        CustomResponse resp = service.openRoom(10, 11);

        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
        DmRoomDTO data = (DmRoomDTO) resp.getData();
        assertEquals(2, data.getRoomId());
        assertEquals(11, data.getPeerId());
    }

    @Test
    void getRoom_whenMissing_returns404() {
        when(dmRoomMapper.selectById(2)).thenReturn(null);

        CustomResponse resp = service.getRoom(10, 2, 1, 50);

        assertEquals(404, resp.getCode());
        assertEquals("会话不存在", resp.getMessage());
    }

    @Test
    void openRoom_whenPeerIdNull_returns400() {
        CustomResponse resp = service.openRoom(10, null);

        assertEquals(400, resp.getCode());
        assertEquals("对方用户 id 不能为空", resp.getMessage());
        verify(dmRoomMapper, never()).insert(any(DmRoom.class));
    }

    @Test
    void openRoom_whenRoomAbsent_createsRoom() {
        when(userMapper.selectById(11)).thenReturn(new User());
        when(dmRoomMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(dmMessageMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<DmMessage> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        CustomResponse resp = service.openRoom(10, 11);

        assertEquals(200, resp.getCode());
        verify(dmRoomMapper).insert(any(DmRoom.class));
        DmRoomDTO data = (DmRoomDTO) resp.getData();
        assertEquals(11, data.getPeerId());
    }

    @Test
    void getRoom_whenNotMember_returns404() {
        DmRoom room = new DmRoom();
        room.setId(2);
        room.setUserA(99);
        room.setUserB(100);
        when(dmRoomMapper.selectById(2)).thenReturn(room);

        CustomResponse resp = service.getRoom(10, 2, 1, 50);

        assertEquals(404, resp.getCode());
        assertEquals("会话不存在", resp.getMessage());
    }

    @Test
    void getRoom_whenMember_returns200() {
        DmRoom room = new DmRoom();
        room.setId(2);
        room.setUserA(10);
        room.setUserB(11);
        when(dmRoomMapper.selectById(2)).thenReturn(room);
        User peer = new User();
        peer.setId(11);
        peer.setNickname("对方昵称");
        when(userMapper.selectById(11)).thenReturn(peer);
        when(dmMessageMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<DmMessage> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        CustomResponse resp = service.getRoom(10, 2, 1, 50);

        assertEquals(200, resp.getCode());
        DmRoomDTO data = (DmRoomDTO) resp.getData();
        assertEquals("对方昵称", data.getPeerNickname());
    }

    @Test
    void send_whenContentTooLong_returns400() {
        String longContent = "a".repeat(501);

        CustomResponse resp = service.send(10, 2, longContent);

        assertEquals(400, resp.getCode());
        assertEquals("消息不能超过500字", resp.getMessage());
        verify(dmMessageMapper, never()).insert(any(DmMessage.class));
    }

    @Test
    void send_whenNotMember_returns404() {
        DmRoom room = new DmRoom();
        room.setId(2);
        room.setUserA(99);
        room.setUserB(100);
        when(dmRoomMapper.selectById(2)).thenReturn(room);

        CustomResponse resp = service.send(10, 2, "你好");

        assertEquals(404, resp.getCode());
        assertEquals("会话不存在", resp.getMessage());
        verify(dmMessageMapper, never()).insert(any(DmMessage.class));
    }
}
