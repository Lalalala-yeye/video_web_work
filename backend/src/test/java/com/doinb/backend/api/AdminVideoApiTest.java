package com.doinb.backend.api;

import com.doinb.backend.controller.AdminVideoController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.dto.VideoReportDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.video.AdminVideoService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 对应测试报告 A001：已登录普通用户访问管理接口。
 * HTTP 仍是 200，业务 {@code code=403}（和未登录的 HTTP 403 不同）。
 */
@WebMvcTest(controllers = AdminVideoController.class)
@ImportApiSecurity
class AdminVideoApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private AdminVideoService adminVideoService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void pending_whenUserToken_returnsJson403() throws Exception {
        User user = new User();
        user.setId(10);
        user.setRole(1);
        when(userMapper.selectById(10)).thenReturn(user);
        when(adminVideoService.listPending(eq(1), anyLong(), anyLong()))
                .thenThrow(new SecurityException("需要管理员权限"));

        String token = jwtUtil.createToken(10, "user");
        mockMvc.perform(get("/admin/video/pending")
                        .param("page", "1")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("需要管理员权限"));
    }

    @Test
    void pending_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(get("/admin/video/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pending_withAdminToken_returnsDefaultPage() throws Exception {
        when(adminVideoService.listPending(2, 1, 10))
                .thenReturn(new PageResult<VideoDTO>(0, 1, 10, List.of()));

        mockMvc.perform(get("/admin/video/pending")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(adminVideoService).listPending(2, 1, 10);
    }

    @Test
    void reportReview_withAdminToken_returnsDefaultPage() throws Exception {
        when(adminVideoService.listReportReview(2, 1, 10))
                .thenReturn(new PageResult<VideoDTO>(0, 1, 10, List.of()));

        mockMvc.perform(get("/admin/video/report-review")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(adminVideoService).listReportReview(2, 1, 10);
    }

    @Test
    void getOne_withAdminToken_returnsServiceBody() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(adminVideoService.getVideoForPreview(2, 12)).thenReturn(body);

        mockMvc.perform(get("/admin/video/getone")
                        .param("id", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminVideoService).getVideoForPreview(2, 12);
    }

    @Test
    void reports_withAdminToken_returnsList() throws Exception {
        when(adminVideoService.listReports(2, 12)).thenReturn(List.<VideoReportDTO>of());

        mockMvc.perform(get("/admin/video/reports")
                        .param("videoId", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());

        verify(adminVideoService).listReports(2, 12);
    }

    @Test
    void approve_withAdminToken_forwardsAdminIdentity() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("审核通过");
        when(adminVideoService.approve(2, 99, 12)).thenReturn(body);

        mockMvc.perform(post("/admin/video/approve")
                        .param("videoId", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("审核通过"));

        verify(adminVideoService).approve(2, 99, 12);
    }

    @Test
    void reject_withAdminToken_forwardsVideoId() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("已驳回");
        when(adminVideoService.reject(2, 12)).thenReturn(body);

        mockMvc.perform(post("/admin/video/reject")
                        .param("videoId", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminVideoService).reject(2, 12);
    }

    @Test
    void delete_withAdminToken_forwardsVideoId() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("删除成功");
        when(adminVideoService.deleteVideo(2, 12)).thenReturn(body);

        mockMvc.perform(post("/admin/video/delete")
                        .param("videoId", "12")
                        .header("Authorization", adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(adminVideoService).deleteVideo(2, 12);
    }

    private String adminToken() {
        User admin = new User();
        admin.setId(99);
        admin.setUsername("admin");
        admin.setRole(2);
        when(userMapper.selectById(99)).thenReturn(admin);
        return "Bearer " + jwtUtil.createToken(99, "admin");
    }
}
