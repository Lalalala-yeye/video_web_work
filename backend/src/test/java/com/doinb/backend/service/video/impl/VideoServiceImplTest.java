package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.PlayHistoryMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReportMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.VideoStatus;
import com.doinb.backend.pojo.dto.ReactionSummaryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.PlayHistory;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.pojo.entity.VideoReport;
import com.doinb.backend.service.reaction.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 V011 / V030 / V031 / V050 / V061 / V063 */
class VideoServiceImplTest {

    private VideoMapper videoMapper;
    private UserMapper userMapper;
    private PlayHistoryMapper playHistoryMapper;
    private VideoReportMapper videoReportMapper;
    private ReactionService reactionService;
    private VideoServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        userMapper = mock(UserMapper.class);
        playHistoryMapper = mock(PlayHistoryMapper.class);
        videoReportMapper = mock(VideoReportMapper.class);
        reactionService = mock(ReactionService.class);
        service = new VideoServiceImpl(
                videoMapper,
                userMapper,
                playHistoryMapper,
                videoReportMapper,
                reactionService
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

    @Test
    void getOne_whenVideoIdNull_returns400() {
        CustomResponse resp = service.getOne(null, 10, 1);

        assertEquals(400, resp.getCode());
        assertEquals("视频 id 不能为空", resp.getMessage());
    }

    @Test
    void getOne_whenPublished_returns200WithReactions() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);
        when(reactionService.getVideoSummary(12, 10)).thenReturn(new ReactionSummaryDTO());

        CustomResponse resp = service.getOne(12, 10, 1);

        assertEquals(200, resp.getCode());
        VideoDTO data = (VideoDTO) resp.getData();
        assertNotNull(data.getReactions());
    }

    @Test
    void saveProgress_whenVideoIdNull_returns400() {
        CustomResponse resp = service.saveProgress(10, null, 10);

        assertEquals(400, resp.getCode());
        assertEquals("视频 id 不能为空", resp.getMessage());
    }

    @Test
    void saveProgress_whenVideoMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.saveProgress(10, 12, 10);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在或未发布", resp.getMessage());
    }

    @Test
    void saveProgress_whenNew_inserts() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);
        when(playHistoryMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        CustomResponse resp = service.saveProgress(10, 12, 60);

        assertEquals(200, resp.getCode());
        assertEquals("进度已保存", resp.getMessage());
        verify(playHistoryMapper).insert(any(PlayHistory.class));
    }

    @Test
    void saveProgress_whenExisting_updates() {
        Video video = new Video();
        video.setId(12);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);
        PlayHistory existing = new PlayHistory();
        existing.setUserId(10);
        existing.setVideoId(12);
        when(playHistoryMapper.selectOne(any(Wrapper.class))).thenReturn(existing);

        CustomResponse resp = service.saveProgress(10, 12, 60);

        assertEquals(200, resp.getCode());
        verify(playHistoryMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void getMyVideo_whenNotOwner_returns403() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(99);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.getMyVideo(10, 1, 12);

        assertEquals(403, resp.getCode());
        assertEquals("无权查看该视频", resp.getMessage());
    }

    @Test
    void getMyVideo_whenOwner_returns200() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        when(videoMapper.selectById(12)).thenReturn(video);
        when(userMapper.selectById(10)).thenReturn(new com.doinb.backend.pojo.entity.User());

        CustomResponse resp = service.getMyVideo(10, 1, 12);

        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
    }

    @Test
    void setVisibility_whenMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.setVisibility(10, 1, 12, "private");

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void setVisibility_whenInvalid_returns400() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.setVisibility(10, 1, 12, "invalid");

        assertEquals(400, resp.getCode());
        assertEquals("可见性参数无效，请使用 public 或 private", resp.getMessage());
    }

    @Test
    void setVisibility_whenPrivate_returns200() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.setVisibility(10, 1, 12, "private");

        assertEquals(200, resp.getCode());
        assertEquals("已设为仅自己可见", resp.getMessage());
    }

    @Test
    void reportVideo_whenVideoIdNull_returns400() {
        CustomResponse resp = service.reportVideo(11, null, "举报");

        assertEquals(400, resp.getCode());
        assertEquals("视频 id 不能为空", resp.getMessage());
    }

    @Test
    void reportVideo_whenAlreadyReported_returns400() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);
        when(videoReportMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        CustomResponse resp = service.reportVideo(11, 12, "再次举报");

        assertEquals(400, resp.getCode());
        assertEquals("您已举报过该视频", resp.getMessage());
    }

    @Test
    void reportVideo_whenThresholdReached_entersReview() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);
        when(videoReportMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 3L);

        CustomResponse resp = service.reportVideo(11, 12, "举报");

        assertEquals(200, resp.getCode());
        assertEquals("举报已提交，该视频已进入复审", resp.getMessage());
    }

    @Test
    void deleteVideo_whenNotOwner_returns403() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(99);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.deleteVideo(10, 1, 12);

        assertEquals(403, resp.getCode());
        assertEquals("无权删除该视频", resp.getMessage());
    }

    @Test
    void deleteVideo_whenOwner_returns200() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.deleteVideo(10, 1, 12);

        assertEquals(200, resp.getCode());
        assertEquals("删除成功", resp.getMessage());
        verify(videoMapper).deleteById(12);
    }

    @Test
    void upload_whenTitleTooLong_returns400() {
        String longTitle = "a".repeat(101);

        CustomResponse resp = service.upload(10, 1, longTitle, "简介", "public", null, null);

        assertEquals(400, resp.getCode());
        assertEquals("标题长度不能超过100", resp.getMessage());
    }

    @Test
    void upload_whenPublic_success(@TempDir Path tempDir) {
        ReflectionTestUtils.setField(service, "uploadPath", tempDir.toString());
        MockMultipartFile file = new MockMultipartFile("file", "a.mp4", "video/mp4", "x".getBytes());

        CustomResponse resp = service.upload(10, 1, "标题", "简介", "public", null, file);

        assertEquals(200, resp.getCode());
        assertEquals("上传成功，等待管理员审核", resp.getMessage());
        verify(videoMapper).insert(any(Video.class));
    }
}
