package com.doinb.backend.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.NotificationMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.Notification;
import com.doinb.backend.pojo.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 N010 / N021 */
class NotificationServiceImplTest {

    private NotificationMapper notificationMapper;
    private VideoMapper videoMapper;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        videoMapper = mock(VideoMapper.class);
        service = new NotificationServiceImpl(
                notificationMapper,
                mock(UserMapper.class),
                videoMapper,
                mock(CommentMapper.class)
        );
    }

    @Test
    void countUnread_returnsMapperCount() {
        when(notificationMapper.selectCount(any(Wrapper.class))).thenReturn(4L);

        assertEquals(4L, service.countUnread(10));
    }

    @Test
    void markRead_whenMissing_returns404() {
        when(notificationMapper.selectById(99)).thenReturn(null);

        CustomResponse resp = service.markRead(10, 99);

        assertEquals(404, resp.getCode());
        assertEquals("通知不存在", resp.getMessage());
    }

    @Test
    void markRead_whenAll_returns200() {
        CustomResponse resp = service.markRead(10, null);

        assertEquals(200, resp.getCode());
        assertEquals("已全部标为已读", resp.getMessage());
        verify(notificationMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void notifyVideoLike_whenSelfLike_doesNotInsert() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        when(videoMapper.selectById(12)).thenReturn(video);

        service.notifyVideoLike(10, 12);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void notifyVideoLike_whenOtherUser_inserts() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setTitle("132");
        when(videoMapper.selectById(12)).thenReturn(video);

        service.notifyVideoLike(5, 12);

        verify(notificationMapper).insert(any(Notification.class));
    }
}
