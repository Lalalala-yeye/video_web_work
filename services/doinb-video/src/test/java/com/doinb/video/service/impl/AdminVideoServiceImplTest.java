package com.doinb.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.video.client.MessageNotifier;
import com.doinb.video.client.UserDirectory;
import com.doinb.video.mapper.VideoMapper;
import com.doinb.video.mapper.VideoReportMapper;
import com.doinb.video.pojo.VideoStatus;
import com.doinb.video.pojo.dto.VideoReportDTO;
import com.doinb.video.pojo.entity.Video;
import com.doinb.video.pojo.entity.VideoReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 A001 / A010 / A011 / A040 */
class AdminVideoServiceImplTest {

    private VideoMapper videoMapper;
    private VideoReportMapper videoReportMapper;
    private UserDirectory userDirectory;
    private MessageNotifier messageNotifier;
    private AdminVideoServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        videoReportMapper = mock(VideoReportMapper.class);
        userDirectory = mock(UserDirectory.class);
        messageNotifier = mock(MessageNotifier.class);
        service = new AdminVideoServiceImpl(
                videoMapper,
                videoReportMapper,
                userDirectory,
                messageNotifier
        );
    }

    @Test
    void approve_whenNotAdmin_returns403() {
        CustomResponse resp = service.approve(1, 10, 12);

        assertEquals(403, resp.getCode());
        assertEquals("需要管理员权限", resp.getMessage());
        verify(videoMapper, never()).updateById(any(Video.class));
        verify(messageNotifier, never()).notifyVideoApproved(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void approve_whenVideoMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.approve(2, 11, 12);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void approve_whenPending_setsPublishedAndNotifies() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setTitle("待审视频");
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.approve(2, 11, 12);

        assertEquals(200, resp.getCode());
        assertEquals("已通过审核", resp.getMessage());
        assertEquals(VideoStatus.PUBLISHED, video.getStatus());
        verify(videoMapper).updateById(video);
        verify(messageNotifier).notifyVideoApproved(11, 10, 12, "待审视频");
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
        video.setAuthorId(10);
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
        UserDTO reporter = new UserDTO();
        reporter.setId(11);
        reporter.setNickname("举报者");
        when(userDirectory.findById(11)).thenReturn(reporter);

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
        verify(messageNotifier, never()).notifyVideoApproved(anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void approve_whenAdminUserIdNull_doesNotNotify() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.approve(2, null, 12);

        assertEquals(200, resp.getCode());
        verify(messageNotifier, never()).notifyVideoApproved(anyInt(), anyInt(), anyInt(), anyString());
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
