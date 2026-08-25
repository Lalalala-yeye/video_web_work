package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.PlayHistoryMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReportMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.VideoStatus;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.pojo.entity.VideoReport;
import com.doinb.backend.service.reaction.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 V011 / V030 / V031 / V050 / V061 / V063 */
class VideoServiceImplTest {

    private VideoMapper videoMapper;
    private VideoReportMapper videoReportMapper;
    private VideoServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        videoReportMapper = mock(VideoReportMapper.class);
        service = new VideoServiceImpl(
                videoMapper,
                mock(UserMapper.class),
                mock(PlayHistoryMapper.class),
                videoReportMapper,
                mock(ReactionService.class)
        );
    }

    @Test
    void getOne_whenMissing_returns404() {
        when(videoMapper.selectById(99999)).thenReturn(null);

        CustomResponse resp = service.getOne(99999, 10, 1);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在或未发布", resp.getMessage());
    }

    @Test
    void getOne_whenPendingAndNotAuthor_returns404() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(99);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.getOne(12, 10, 1);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在或未发布", resp.getMessage());
    }

    @Test
    void saveProgress_whenInvalid_returns400() {
        CustomResponse resp = service.saveProgress(10, 12, -1);

        assertEquals(400, resp.getCode());
        assertEquals("播放进度无效", resp.getMessage());
    }

    @Test
    void upload_whenTitleBlank_returns400() {
        CustomResponse resp = service.upload(10, 1, "  ", "简介", "public", null, null);

        assertEquals(400, resp.getCode());
        assertEquals("标题不能为空", resp.getMessage());
    }

    @Test
    void upload_whenFileMissing_returns400() {
        CustomResponse resp = service.upload(10, 1, "测试视频标题", "简介", "public", null, null);

        assertEquals(400, resp.getCode());
        assertEquals("请上传视频文件", resp.getMessage());
    }

    @Test
    void upload_whenFormatInvalid_returns400() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        CustomResponse resp = service.upload(10, 1, "测试视频标题", "简介", "public", null, file);

        assertEquals(400, resp.getCode());
        assertEquals("视频格式仅支持 mp4 / webm / mov", resp.getMessage());
        verify(videoMapper, never()).insert(any(Video.class));
    }

    @Test
    void setVisibility_whenNotOwner_returns403() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(99);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.setVisibility(10, 1, 12, "private");

        assertEquals(403, resp.getCode());
        assertEquals("无权修改该视频", resp.getMessage());
    }

    @Test
    void reportVideo_whenNotPublished_returns404() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.reportVideo(11, 12, "测试举报");

        assertEquals(404, resp.getCode());
        assertEquals("只能举报已发布的视频", resp.getMessage());
    }

    @Test
    void reportVideo_whenSelf_returns400() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.reportVideo(10, 12, "测试举报");

        assertEquals(400, resp.getCode());
        assertEquals("不能举报自己的视频", resp.getMessage());
    }

    @Test
    void reportVideo_whenFirstTime_returns200() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);
        when(videoReportMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 1L);

        CustomResponse resp = service.reportVideo(11, 12, "测试举报");

        assertEquals(200, resp.getCode());
        assertEquals("举报已提交", resp.getMessage());
        verify(videoReportMapper).insert(any(VideoReport.class));
    }
}
