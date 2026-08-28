package com.doinb.backend.api;

import com.doinb.backend.controller.VideoController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.PlayHistoryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.video.VideoService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 对应测试报告 V011（公开详情 404）/ V031（已登录但缺 file → 业务 400） */
@WebMvcTest(controllers = VideoController.class)
@ImportApiSecurity
class VideoApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private VideoService videoService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void getOne_whenMissing_returns404() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setCode(404);
        body.setMessage("视频不存在或未发布");
        when(videoService.getOne(eq(99999), nullable(Integer.class), nullable(Integer.class))).thenReturn(body);

        mockMvc.perform(get("/video/getone").param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("视频不存在或未发布"));
    }

    @Test
    void upload_withoutFile_returns400() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setCode(400);
        body.setMessage("请上传视频文件");
        when(videoService.upload(eq(10), eq(1), eq("测试视频标题"), nullable(String.class), eq("public"),
                nullable(MultipartFile.class), nullable(MultipartFile.class))).thenReturn(body);

        mockMvc.perform(multipart("/video/upload")
                        .param("title", "测试视频标题")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("请上传视频文件"));

        verify(videoService).upload(eq(10), eq(1), eq("测试视频标题"), nullable(String.class), eq("public"),
                nullable(MultipartFile.class), nullable(MultipartFile.class));
    }

    @Test
    void list_withoutToken_returnsDefaultPage() throws Exception {
        when(videoService.listPublished(1, 12))
                .thenReturn(new PageResult<VideoDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/video/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(videoService).listPublished(1, 12);
    }

    @Test
    void list_withCustomPaging_serializesMultipleRecords() throws Exception {
        when(videoService.listPublished(2, 2))
                .thenReturn(new PageResult<VideoDTO>(5, 2, 2, List.of(new VideoDTO(), new VideoDTO())));

        mockMvc.perform(get("/video/list")
                        .param("page", "2")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.records.length()").value(2));

        verify(videoService).listPublished(2, 2);
    }

    @Test
    void getOne_whenPublished_returnsServiceBody() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(videoService.getOne(12, null, null)).thenReturn(body);

        mockMvc.perform(get("/video/getone").param("id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(videoService).getOne(12, null, null);
    }

    @Test
    void saveProgress_withToken_forwardsParameters() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(videoService.saveProgress(10, 12, 30)).thenReturn(body);

        mockMvc.perform(post("/video/history/progress")
                        .param("videoId", "12")
                        .param("progress", "30")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(videoService).saveProgress(10, 12, 30);
    }

    @Test
    void historyList_withToken_returnsDefaultPage() throws Exception {
        when(videoService.listHistory(10, 1, 12))
                .thenReturn(new PageResult<PlayHistoryDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/video/history/list")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(videoService).listHistory(10, 1, 12);
    }

    @Test
    void upload_withToken_forwardsMultipartData() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("上传成功");
        when(videoService.upload(eq(10), eq(1), eq("测试标题"), eq("简介"), eq("public"),
                nullable(MultipartFile.class), any(MultipartFile.class))).thenReturn(body);
        MockMultipartFile file = new MockMultipartFile(
                "file", "demo.mp4", "video/mp4", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/video/upload")
                        .file(file)
                        .param("title", "测试标题")
                        .param("description", "简介")
                        .param("visibility", "public")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"));

        verify(videoService).upload(eq(10), eq(1), eq("测试标题"), eq("简介"), eq("public"),
                nullable(MultipartFile.class), any(MultipartFile.class));
    }

    @Test
    void myList_withToken_returnsDefaultPage() throws Exception {
        when(videoService.listMyVideos(10, 1, 12))
                .thenReturn(new PageResult<VideoDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/video/my/list")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(videoService).listMyVideos(10, 1, 12);
    }

    @Test
    void update_withToken_forwardsFormData() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("修改成功");
        when(videoService.updateVideo(10, 1, 12, "新标题", "简介", "private", null, null))
                .thenReturn(body);

        mockMvc.perform(post("/video/update")
                        .param("id", "12")
                        .param("title", "新标题")
                        .param("description", "简介")
                        .param("visibility", "private")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));

        verify(videoService).updateVideo(10, 1, 12, "新标题", "简介", "private", null, null);
    }

    @Test
    void myGetOne_withToken_returnsServiceBody() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(videoService.getMyVideo(10, 1, 12)).thenReturn(body);

        mockMvc.perform(get("/video/my/getone")
                        .param("id", "12")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(videoService).getMyVideo(10, 1, 12);
    }

    @Test
    void visibility_withToken_forwardsParameters() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("设置成功");
        when(videoService.setVisibility(10, 1, 12, "private")).thenReturn(body);

        mockMvc.perform(post("/video/visibility")
                        .param("id", "12")
                        .param("visibility", "private")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(videoService).setVisibility(10, 1, 12, "private");
    }

    @Test
    void visibility_whenValueInvalid_returnsServiceValidation() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setCode(400);
        body.setMessage("visibility 参数非法");
        when(videoService.setVisibility(10, 1, 12, "unknown")).thenReturn(body);

        mockMvc.perform(post("/video/visibility")
                        .param("id", "12")
                        .param("visibility", "unknown")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("visibility 参数非法"));

        verify(videoService).setVisibility(10, 1, 12, "unknown");
    }

    @Test
    void report_withToken_forwardsReason() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("举报成功");
        when(videoService.reportVideo(10, 12, "违规内容")).thenReturn(body);

        mockMvc.perform(post("/video/report")
                        .param("id", "12")
                        .param("reason", "违规内容")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(videoService).reportVideo(10, 12, "违规内容");
    }

    @Test
    void delete_withToken_forwardsCurrentUser() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("删除成功");
        when(videoService.deleteVideo(10, 1, 12)).thenReturn(body);

        mockMvc.perform(post("/video/delete")
                        .param("id", "12")
                        .header("Authorization", userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(videoService).deleteVideo(10, 1, 12);
    }

    private String userToken() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setRole(1);
        when(userMapper.selectById(10)).thenReturn(user);
        return "Bearer " + jwtUtil.createToken(10, "user");
    }
}
