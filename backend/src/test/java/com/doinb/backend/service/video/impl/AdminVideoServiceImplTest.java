package com.doinb.backend.service.video.impl;

import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReportMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.VideoStatus;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 A001 / A010 / A011 / A040 */
class AdminVideoServiceImplTest {

    private VideoMapper videoMapper;
    private NotificationService notificationService;
    private AdminVideoServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        notificationService = mock(NotificationService.class);
        service = new AdminVideoServiceImpl(
                videoMapper,
                mock(UserMapper.class),
                mock(VideoReportMapper.class),
                notificationService
        );
    }

    @Test
    void approve_whenNotAdmin_returns403() {
        CustomResponse resp = service.approve(1, 10, 12);

        assertEquals(403, resp.getCode());
        assertEquals("需要管理员权限", resp.getMessage());
        verify(videoMapper, never()).updateById(any(Video.class));
    }

    @Test
    void approve_whenVideoMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.approve(2, 11, 12);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void approve_whenPending_setsPublished() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.approve(2, 11, 12);

        assertEquals(200, resp.getCode());
        assertEquals("已通过审核", resp.getMessage());
        assertEquals(VideoStatus.PUBLISHED, video.getStatus());
        verify(videoMapper).updateById(video);
        verify(notificationService).notifyVideoApproved(11, 12);
    }

    @Test
    void reject_whenPending_setsPrivate() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.reject(2, 12);

        assertEquals(200, resp.getCode());
        assertEquals("已驳回，视频设为仅自己可见", resp.getMessage());
        assertEquals(VideoStatus.PRIVATE, video.getStatus());
        verify(videoMapper).updateById(video);
    }

    @Test
    void reject_whenNotAdmin_returns403() {
        CustomResponse resp = service.reject(1, 12);

        assertEquals(403, resp.getCode());
        assertEquals("需要管理员权限", resp.getMessage());
        verify(videoMapper, never()).updateById(any(Video.class));
    }

    @Test
    void deleteVideo_whenExists_returns200() {
        Video video = new Video();
        video.setId(12);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.deleteVideo(2, 12);

        assertEquals(200, resp.getCode());
        assertEquals("已删除", resp.getMessage());
        verify(videoMapper).deleteById(12);
    }

    @Test
    void listPending_whenNotAdmin_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                SecurityException.class,
                () -> service.listPending(1, 1, 10)
        );
    }
}
