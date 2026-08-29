package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.CommentReactionMapper;
import com.doinb.backend.mapper.PlayHistoryMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReactionMapper;
import com.doinb.backend.mapper.VideoReportMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.VideoStatus;
import com.doinb.backend.pojo.dto.VideoReportDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.pojo.entity.VideoReport;
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

/** 对应测试报告 A001 / A010 / A011 / A040 */
class AdminVideoServiceImplTest {

    private VideoMapper videoMapper;
    private UserMapper userMapper;
    private VideoReportMapper videoReportMapper;
    private PlayHistoryMapper playHistoryMapper;
    private VideoReactionMapper videoReactionMapper;
    private CommentMapper commentMapper;
    private CommentReactionMapper commentReactionMapper;
    private NotificationService notificationService;
    private AdminVideoServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        userMapper = mock(UserMapper.class);
        videoReportMapper = mock(VideoReportMapper.class);
        playHistoryMapper = mock(PlayHistoryMapper.class);
        videoReactionMapper = mock(VideoReactionMapper.class);
        commentMapper = mock(CommentMapper.class);
        commentReactionMapper = mock(CommentReactionMapper.class);
        notificationService = mock(NotificationService.class);
        service = new AdminVideoServiceImpl(
                videoMapper,
                userMapper,
                videoReportMapper,
                playHistoryMapper,
                videoReactionMapper,
                commentMapper,
                commentReactionMapper,
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

    @Test
    void getVideoForPreview_whenNotAdmin_returns403() {
        CustomResponse resp = service.getVideoForPreview(1, 12);

        assertEquals(403, resp.getCode());
        assertEquals("需要管理员权限", resp.getMessage());
    }

    @Test
    void getVideoForPreview_whenVideoIdNull_returns400() {
        CustomResponse resp = service.getVideoForPreview(2, null);

        assertEquals(400, resp.getCode());
        assertEquals("视频 id 不能为空", resp.getMessage());
    }

    @Test
    void getVideoForPreview_whenMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.getVideoForPreview(2, 12);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void getVideoForPreview_whenExists_returns200() {
        Video video = new Video();
        video.setId(12);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.getVideoForPreview(2, 12);

        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
    }

    @Test
    void listReports_whenNotAdmin_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                SecurityException.class,
                () -> service.listReports(1, 12)
        );
    }

    @Test
    void listReports_whenVideoIdNull_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.listReports(2, null)
        );
    }

    @Test
    void listReports_whenHasReports_returnsList() {
        VideoReport report = new VideoReport();
        report.setId(1);
        report.setVideoId(12);
        report.setReporterId(11);
        report.setReason("原因");
        when(videoReportMapper.selectList(any(Wrapper.class))).thenReturn(List.of(report));
        User reporter = new User();
        reporter.setId(11);
        reporter.setNickname("举报者");
        when(userMapper.selectById(11)).thenReturn(reporter);

        List<VideoReportDTO> list = service.listReports(2, 12);

        assertEquals(1, list.size());
        assertEquals("举报者", list.get(0).getReporterNickname());
    }

    @Test
    void approve_whenAlreadyPublished_returns400() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.approve(2, 11, 12);

        assertEquals(400, resp.getCode());
        assertEquals("当前状态不可审核通过", resp.getMessage());
    }

    @Test
    void approve_whenAdminUserIdNull_doesNotNotify() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.approve(2, null, 12);

        assertEquals(200, resp.getCode());
        verify(notificationService, never()).notifyVideoApproved(any(), any());
    }

    @Test
    void reject_whenMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.reject(2, 12);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void deleteVideo_whenNotAdmin_returns403() {
        CustomResponse resp = service.deleteVideo(1, 12);

        assertEquals(403, resp.getCode());
        assertEquals("需要管理员权限", resp.getMessage());
    }

    @Test
    void deleteVideo_whenMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.deleteVideo(2, 12);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }
}
