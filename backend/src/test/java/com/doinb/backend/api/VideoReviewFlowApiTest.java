package com.doinb.backend.api;

import com.doinb.backend.controller.AdminVideoController;
import com.doinb.backend.controller.VideoController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.video.AdminVideoService;
import com.doinb.backend.service.video.VideoService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 审核状态变化后的公开可见性主流程。 */
@WebMvcTest(controllers = {AdminVideoController.class, VideoController.class})
@ImportApiSecurity
class VideoReviewFlowApiTest {

    private static final int PENDING = 0;
    private static final int PUBLISHED = 1;
    private static final int PRIVATE = 3;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AdminVideoService adminVideoService;
    @MockitoBean
    private VideoService videoService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void approve_thenVideoIsVisibleInPublicListAndDetail() throws Exception {
        VideoDTO video = video(PENDING);
        when(adminVideoService.listPending(2, 1, 10))
                .thenAnswer(invocation -> new PageResult<>(1, 1, 10, List.of(video)));
        when(adminVideoService.approve(2, 99, 12)).thenAnswer(invocation -> {
            video.setStatus(PUBLISHED);
            return response(200, "已通过审核", null);
        });
        when(videoService.listPublished(1, 12)).thenAnswer(invocation ->
                video.getStatus() == PUBLISHED
                        ? new PageResult<>(1, 1, 12, List.of(video))
                        : new PageResult<>(0, 1, 12, List.of()));
        when(videoService.getOne(eq(12), nullable(Integer.class), nullable(Integer.class)))
                .thenAnswer(invocation -> video.getStatus() == PUBLISHED
                        ? response(200, "OK", video)
                        : response(404, "视频不存在或未发布", null));

        mockMvc.perform(get("/admin/video/pending")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].status").value(PENDING));

        mockMvc.perform(post("/admin/video/approve")
                        .param("videoId", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/video/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(12))
                .andExpect(jsonPath("$.data.records[0].status").value(PUBLISHED));

        mockMvc.perform(get("/video/getone").param("id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(12));

        verify(adminVideoService).approve(2, 99, 12);
    }

    @Test
    void reject_thenVideoIsHiddenPubliclyButVisibleToOwner() throws Exception {
        VideoDTO video = video(PENDING);
        when(adminVideoService.reject(2, 12)).thenAnswer(invocation -> {
            video.setStatus(PRIVATE);
            return response(200, "已驳回，视频设为仅自己可见", null);
        });
        when(videoService.listPublished(1, 12)).thenAnswer(invocation ->
                video.getStatus() == PUBLISHED
                        ? new PageResult<>(1, 1, 12, List.of(video))
                        : new PageResult<>(0, 1, 12, List.of()));
        when(videoService.getOne(eq(12), nullable(Integer.class), nullable(Integer.class)))
                .thenAnswer(invocation -> video.getStatus() == PUBLISHED
                        ? response(200, "OK", video)
                        : response(404, "视频不存在或未发布", null));
        when(videoService.getMyVideo(10, 1, 12)).thenAnswer(invocation ->
                video.getStatus() == PRIVATE
                        ? response(200, "OK", video)
                        : response(404, "视频不存在", null));

        mockMvc.perform(post("/admin/video/reject")
                        .param("videoId", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/video/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());

        mockMvc.perform(get("/video/getone").param("id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(get("/video/my/getone")
                        .param("id", "12")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.status").value(PRIVATE));

        verify(adminVideoService).reject(2, 12);
        verify(videoService).getMyVideo(10, 1, 12);
    }

    private VideoDTO video(int status) {
        VideoDTO video = new VideoDTO();
        video.setId(12);
        video.setTitle("待审核视频");
        video.setAuthorId(10);
        video.setStatus(status);
        return video;
    }

    private CustomResponse response(int code, String message, Object data) {
        return new CustomResponse(code, message, data);
    }

    private String adminToken() {
        User admin = user(99, 2, "admin");
        when(userMapper.selectById(99)).thenReturn(admin);
        return "Bearer " + jwtUtil.createToken(99, "admin");
    }

    private String userToken() {
        User user = user(10, 1, "user_a");
        when(userMapper.selectById(10)).thenReturn(user);
        return "Bearer " + jwtUtil.createToken(10, "user");
    }

    private User user(int id, int role, String username) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setUsername(username);
        return user;
    }
}
