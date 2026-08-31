package com.doinb.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.message.client.UserDirectoryClient;
import com.doinb.message.mapper.DmMessageMapper;
import com.doinb.message.mapper.DmRoomMapper;
import com.doinb.message.pojo.dto.DmMessageDTO;
import com.doinb.message.pojo.dto.DmRoomDTO;
import com.doinb.message.pojo.entity.DmMessage;
import com.doinb.message.pojo.entity.DmRoom;
import com.doinb.message.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageServiceImplTest {

    private DmRoomMapper dmRoomMapper;
    private DmMessageMapper dmMessageMapper;
    private UserDirectoryClient userDirectoryClient;
    private NotificationService notificationService;
    private MessageServiceImpl service;

    @BeforeEach
    void setUp() {
        dmRoomMapper = mock(DmRoomMapper.class);
        dmMessageMapper = mock(DmMessageMapper.class);
        userDirectoryClient = mock(UserDirectoryClient.class);
        notificationService = mock(NotificationService.class);
        service = new MessageServiceImpl(
                dmRoomMapper, dmMessageMapper, userDirectoryClient, notificationService);
    }

    @Test
    void openRoom_whenSelf_returns400() {
        CustomResponse response = service.openRoom(10, 10);

        assertEquals(400, response.getCode());
        assertEquals("不能给自己发私信", response.getMessage());
        verify(dmRoomMapper, never()).insert(any(DmRoom.class));
    }

    @Test
    void openRoom_whenPeerMissing_returns404() {
        when(userDirectoryClient.findById(11)).thenReturn(null);

        CustomResponse response = service.openRoom(10, 11);

        assertEquals(404, response.getCode());
        assertEquals("用户不存在", response.getMessage());
    }

    @Test
    void openRoom_whenAbsent_createsCanonicalRoom() {
        UserDTO peer = user(2, "对方");
        when(userDirectoryClient.findById(2)).thenReturn(peer);
        when(dmRoomMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(dmRoomMapper.insert(any(DmRoom.class))).thenAnswer(invocation -> {
            invocation.<DmRoom>getArgument(0).setId(20);
            return 1;
        });
        emptyMessagePage();
        when(userDirectoryClient.findByIds(any())).thenReturn(Map.of(2, peer));

        CustomResponse response = service.openRoom(10, 2);

        assertEquals(200, response.getCode());
        ArgumentCaptor<DmRoom> roomCaptor = ArgumentCaptor.forClass(DmRoom.class);
        verify(dmRoomMapper).insert(roomCaptor.capture());
        assertEquals(2, roomCaptor.getValue().getUserA());
        assertEquals(10, roomCaptor.getValue().getUserB());
        DmRoomDTO data = (DmRoomDTO) response.getData();
        assertEquals(20, data.getRoomId());
        assertEquals("对方", data.getPeerNickname());
    }

    @Test
    void getRoom_whenNotMember_returns404() {
        DmRoom room = room(20, 2, 3);
        when(dmRoomMapper.selectById(20)).thenReturn(room);

        CustomResponse response = service.getRoom(10, 20, 1, 50);

        assertEquals(404, response.getCode());
        assertEquals("会话不存在", response.getMessage());
    }

    @Test
    void getRoom_returnsMessagesWithUserDisplayData() {
        DmRoom room = room(20, 10, 11);
        when(dmRoomMapper.selectById(20)).thenReturn(room);
        DmMessage message = new DmMessage();
        message.setId(30);
        message.setRoomId(20);
        message.setSenderId(10);
        message.setContent("你好");
        when(dmMessageMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<DmMessage> page = invocation.getArgument(0);
            page.setRecords(List.of(message));
            page.setTotal(1);
            return page;
        });
        when(userDirectoryClient.findByIds(any())).thenReturn(Map.of(
                10, user(10, "我"), 11, user(11, "对方")));

        CustomResponse response = service.getRoom(10, 20, 1, 50);

        DmRoomDTO data = (DmRoomDTO) response.getData();
        assertEquals("对方", data.getPeerNickname());
        assertEquals(1, data.getMessages().size());
        assertEquals(true, data.getMessages().get(0).getMine());
        assertEquals("我", data.getMessages().get(0).getSenderNickname());
    }

    @Test
    void send_whenSuccessful_persistsAndCreatesType3Notification() {
        DmRoom room = room(20, 10, 11);
        when(dmRoomMapper.selectById(20)).thenReturn(room);
        when(dmMessageMapper.insert(any(DmMessage.class))).thenAnswer(invocation -> {
            invocation.<DmMessage>getArgument(0).setId(31);
            return 1;
        });
        when(userDirectoryClient.findById(10)).thenReturn(user(10, "发送者"));

        CustomResponse response = service.send(10, 20, "  你好，对方  ");

        assertEquals(200, response.getCode());
        assertEquals("发送成功", response.getMessage());
        DmMessageDTO data = (DmMessageDTO) response.getData();
        assertEquals("你好，对方", data.getContent());
        ArgumentCaptor<DmMessage> messageCaptor = ArgumentCaptor.forClass(DmMessage.class);
        verify(dmMessageMapper).insert(messageCaptor.capture());
        assertEquals("你好，对方", messageCaptor.getValue().getContent());
        verify(dmRoomMapper).updateById(room);
        verify(notificationService).notifyMessage(10, 11, 20, "你好，对方");
    }

    @Test
    void send_rejectsBlankLongAndMissingRoom() {
        assertEquals(400, service.send(10, 20, "  ").getCode());
        assertEquals(400, service.send(10, 20, "a".repeat(501)).getCode());
        when(dmRoomMapper.selectById(20)).thenReturn(null);
        assertEquals(404, service.send(10, 20, "你好").getCode());

        verify(dmMessageMapper, never()).insert(any(DmMessage.class));
        verify(notificationService, never()).notifyMessage(any(), any(), any(), any());
    }

    private void emptyMessagePage() {
        when(dmMessageMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<DmMessage> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });
    }

    private static DmRoom room(int id, int userA, int userB) {
        DmRoom room = new DmRoom();
        room.setId(id);
        room.setUserA(userA);
        room.setUserB(userB);
        return room;
    }

    private static UserDTO user(int id, String nickname) {
        return new UserDTO(id, "user" + id, nickname, "/" + id + ".png", 1, null);
    }
}
