package com.doinb.backend.api;

import com.doinb.backend.controller.AdminVideoController;
import com.doinb.backend.mapper.UserMapper;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
