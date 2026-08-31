package com.doinb.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.CreateNotificationRequest;
import com.doinb.common.dto.UserDTO;
import com.doinb.message.client.UserDirectoryClient;
import com.doinb.message.mapper.NotificationMapper;
import com.doinb.message.pojo.dto.NotificationDTO;
import com.doinb.message.pojo.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationServiceImplTest {

    private NotificationMapper notificationMapper;
    private UserDirectoryClient userDirectoryClient;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        userDirectoryClient = mock(UserDirectoryClient.class);
        service = new NotificationServiceImpl(notificationMapper, userDirectoryClient);
    }

    @Test
    void create_withSuppliedLinkPath_persistsNotification() {
        CreateNotificationRequest request = request(12, 2, 7, 30, "/video/99");
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            invocation.<Notification>getArgument(0).setId(81);
            return 1;
        });

        CustomResponse response = service.create(request);

        assertEquals(200, response.getCode());
        assertEquals("通知创建成功", response.getMessage());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("/video/99", saved.getLinkPath());
        assertEquals(false, saved.getIsRead());
    }

    @Test
    void create_whenSelfAction_returns200WithoutInsert() {
        CustomResponse response = service.create(request(12, 1, 12, 30, null));

        assertEquals(200, response.getCode());
        assertEquals("已忽略本人操作", response.getMessage());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void create_withoutLinkPath_buildsFallback() {
        CreateNotificationRequest request = request(12, 4, 1, 30, null);
        when(notificationMapper.insert(any(Notification.class))).thenAnswer(invocation -> {
            invocation.<Notification>getArgument(0).setId(82);
            return 1;
        });

        service.create(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(captor.capture());
        assertEquals("/video/30", captor.getValue().getLinkPath());
    }

    @Test
    void create_withInvalidType_returns400() {
        CustomResponse response = service.create(request(12, 9, 1, 30, null));

        assertEquals(400, response.getCode());
        assertEquals("通知类型无效", response.getMessage());
        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void list_enrichesUsersAndKeepsSystemActorName() {
        Notification like = notification(1, 1, 7, 30, "/video/30");
        Notification approved = notification(2, 4, 9, 31, null);
        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Notification> page = invocation.getArgument(0);
            page.setRecords(List.of(like, approved));
            page.setTotal(2);
            return page;
        });
        UserDTO actor = new UserDTO(7, "u7", "用户七", "/a.png", 1, null);
        when(userDirectoryClient.findByIds(any())).thenReturn(Map.of(7, actor));

        PageResult<NotificationDTO> result = service.list(12, 0, 500);

        assertEquals(1, result.getPage());
        assertEquals(50, result.getSize());
        assertEquals("用户七", result.getRecords().get(0).getActorNickname());
        assertEquals("/a.png", result.getRecords().get(0).getActorAvatar());
        assertEquals("doinb", result.getRecords().get(1).getActorNickname());
        assertNull(result.getRecords().get(1).getActorAvatar());
        assertEquals("/video/31", result.getRecords().get(1).getLinkPath());
    }

    @Test
    void unreadAndMarkRead_keepOwnershipRules() {
        when(notificationMapper.selectCount(any(Wrapper.class))).thenReturn(3L);
        Notification otherUsers = new Notification();
        otherUsers.setId(5);
        otherUsers.setUserId(99);
        when(notificationMapper.selectById(5)).thenReturn(otherUsers);

        assertEquals(3L, service.countUnread(12));
        CustomResponse response = service.markRead(12, 5);
        assertEquals(404, response.getCode());

        CustomResponse readAll = service.markRead(12, null);
        assertEquals("已全部标为已读", readAll.getMessage());
        verify(notificationMapper).update(any(), any(Wrapper.class));
    }

    private static CreateNotificationRequest request(int userId, int type, int actorId,
                                                     int refId, String linkPath) {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(userId);
        request.setType(type);
        request.setActorId(actorId);
        request.setRefId(refId);
        request.setPreview("测试通知");
        request.setLinkPath(linkPath);
        return request;
    }

    private static Notification notification(int id, int type, int actorId,
                                             int refId, String linkPath) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setType(type);
        notification.setActorId(actorId);
        notification.setRefId(refId);
        notification.setPreview("测试");
        notification.setLinkPath(linkPath);
        notification.setIsRead(false);
        return notification;
    }
}
