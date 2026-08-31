package com.doinb.video.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.GatewayHeaders;
import com.doinb.common.PageResult;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.VideoDTO;
import com.doinb.common.web.DownstreamAuthFilter;
import com.doinb.video.internal.InternalVideoController;
import com.doinb.video.service.AdminVideoService;
import com.doinb.video.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class VideoApiContractTest {

    private VideoService videoService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        videoService = mock(VideoService.class);
        AdminVideoService adminVideoService = mock(AdminVideoService.class);
        DoinbProperties properties = new DoinbProperties();
        properties.setRole("service");
        properties.setInternalToken("test-internal-token");
        properties.setPublicPathPrefixes(List.of(
                "/health",
                "/video/list",
                "/video/getone",
                "/uploads/videos",
                "/uploads/covers"));
        mockMvc = standaloneSetup(
                new VideoController(videoService),
                new AdminVideoController(adminVideoService),
                new InternalVideoController(videoService))
                .addFilters(new DownstreamAuthFilter(properties))
                .build();
    }

    @Test
    void publicListAndGetOne_withoutUserHeader_return200() throws Exception {
        VideoDTO row = new VideoDTO();
        row.setId(12);
        row.setTitle("已发布");
        when(videoService.listPublished(1, 12)).thenReturn(new PageResult<>(1, 1, 12, List.of(row)));
        when(videoService.getOne(eq(12), isNull(), isNull()))
                .thenReturn(CustomResponse.ok("OK", row));

        mockMvc.perform(get("/video/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].title").value("已发布"));
        mockMvc.perform(get("/video/getone").param("id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(12));
    }

    @Test
    void protectedEndpoints_withoutUserHeader_return403() throws Exception {
        mockMvc.perform(get("/video/history/list"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/video/upload").param("title", "标题"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/video/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalGetOne_withoutToken_return403() throws Exception {
        mockMvc.perform(get("/internal/videos/12"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalGetOne_whenMissing_returns404Body() throws Exception {
        when(videoService.getInternal(12)).thenReturn(null);

        mockMvc.perform(get("/internal/videos/12")
                        .header(GatewayHeaders.INTERNAL_TOKEN, "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("视频不存在"));
    }

    @Test
    void internalGetOne_whenExists_returns200() throws Exception {
        VideoDTO video = new VideoDTO();
        video.setId(12);
        video.setTitle("待审");
        when(videoService.getInternal(12)).thenReturn(video);

        mockMvc.perform(get("/internal/videos/12")
                        .header(GatewayHeaders.INTERNAL_TOKEN, "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(12));
    }

    @Test
    void internalSearchAndByAuthors_keepPaths() throws Exception {
        VideoDTO video = new VideoDTO();
        video.setId(12);
        video.setTitle("春季赛");
        when(videoService.searchPublished("春季", 10)).thenReturn(List.of(video));
        when(videoService.listPublishedByAuthors(eq(List.of(10, 11)), anyLong()))
                .thenReturn(List.of(video));

        mockMvc.perform(get("/internal/search/videos")
                        .param("keyword", "春季")
                        .header(GatewayHeaders.INTERNAL_TOKEN, "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("春季赛"));
        mockMvc.perform(get("/internal/videos/by-authors")
                        .param("authorIds", "10,11")
                        .header(GatewayHeaders.INTERNAL_TOKEN, "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(12));
    }
}
