package com.doinb.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.dto.VideoDTO;
import com.doinb.video.client.UserDirectory;
import com.doinb.video.mapper.PlayHistoryMapper;
import com.doinb.video.mapper.VideoMapper;
import com.doinb.video.mapper.VideoReportMapper;
import com.doinb.video.pojo.VideoStatus;
import com.doinb.video.pojo.entity.PlayHistory;
import com.doinb.video.pojo.entity.Video;
import com.doinb.video.pojo.entity.VideoReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 V011 / V030 / V031 / V050 / V061 / V063 */
class VideoServiceImplTest {

    private VideoMapper videoMapper;
    private PlayHistoryMapper playHistoryMapper;
    private VideoReportMapper videoReportMapper;
    private UserDirectory userDirectory;
    private VideoServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        playHistoryMapper = mock(PlayHistoryMapper.class);
        videoReportMapper = mock(VideoReportMapper.class);
        userDirectory = mock(UserDirectory.class);
        service = new VideoServiceImpl(
                videoMapper,
                playHistoryMapper,
                videoReportMapper,
                userDirectory
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
    void getOne_whenPublished_returns200WithoutEmbeddedReactions() {
        Video video = new Video();
        video.setId(12);
        video.setTitle("已发布");
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectById(12)).thenReturn(video);

        CustomResponse resp = service.getOne(12, 10, 1);

        assertEquals(200, resp.getCode());
        VideoDTO data = (VideoDTO) resp.getData();
        assertEquals(12, data.getId());
        assertEquals("未知作者", data.getAuthorNickname());
    }

    @Test
    void getOne_whenPendingAndAuthor_returns200() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);
        UserDTO author = new UserDTO();
        author.setId(10);
        author.setNickname("作者甲");
        when(userDirectory.findById(10)).thenReturn(author);

        CustomResponse resp = service.getOne(12, 10, 1);

        assertEquals(200, resp.getCode());
        VideoDTO data = (VideoDTO) resp.getData();
        assertEquals("作者甲", data.getAuthorNickname());
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

    @Test
    void getInternal_whenMissing_returnsNull() {
        when(videoMapper.selectById(12)).thenReturn(null);
        assertNull(service.getInternal(12));
    }

    @Test
    void getInternal_whenExists_returnsDtoEvenIfUnpublished() {
        Video video = new Video();
        video.setId(12);
        video.setTitle("待审");
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PENDING);
        when(videoMapper.selectById(12)).thenReturn(video);

        VideoDTO dto = service.getInternal(12);

        assertNotNull(dto);
        assertEquals(12, dto.getId());
        assertEquals(VideoStatus.PENDING, dto.getStatus());
    }

    @Test
    void listPublishedByAuthors_whenEmptyIds_returnsEmpty() {
        assertTrue(service.listPublishedByAuthors(List.of(), 10).isEmpty());
        verify(videoMapper, never()).selectList(any());
    }

    @Test
    void searchPublished_whenBlank_returnsEmpty() {
        assertTrue(service.searchPublished("  ", 10).isEmpty());
        verify(videoMapper, never()).selectList(any());
    }

    @Test
    void searchPublished_whenMatched_returnsList() {
        Video video = new Video();
        video.setId(12);
        video.setTitle("春季赛");
        video.setAuthorId(10);
        video.setStatus(VideoStatus.PUBLISHED);
        when(videoMapper.selectList(any(Wrapper.class))).thenReturn(List.of(video));

        List<VideoDTO> list = service.searchPublished("春季", 10);

        assertEquals(1, list.size());
        assertEquals(12, list.get(0).getId());
    }
}
