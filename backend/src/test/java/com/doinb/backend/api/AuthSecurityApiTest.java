package com.doinb.backend.api;

import com.doinb.backend.controller.UserAccountController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserAccountService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 示例：鉴权相关。对应 U031（未登录 HTTP 403）和带 JWT 后进入 Controller。
 * <p>
 * 两种 403 不要搞混：没 Token 是 HTTP 403；有 Token 但业务失败是 HTTP 200 + {@code $.code=403}。
 */
@WebMvcTest(controllers = UserAccountController.class)
@ImportApiSecurity
class AuthSecurityApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserAccountService userAccountService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void personalInfo_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(get("/user/personal/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    void personalInfo_withInvalidToken_returnsHttp403() throws Exception {
        mockMvc.perform(get("/user/personal/info")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void personalInfo_withUserToken_returnsServiceBody() throws Exception {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setRole(1);
        when(userMapper.selectById(10)).thenReturn(user);

        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(userAccountService.personalInfo()).thenReturn(body);

        String token = jwtUtil.createToken(10, "user");
        mockMvc.perform(get("/user/personal/info")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void login_jsonBody_returnsServiceCode() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setCode(403);
        body.setMessage("账号或密码不正确");
        when(userAccountService.login("user_a", "wrong_pass")).thenReturn(body);

        mockMvc.perform(post("/user/account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user_a\",\"password\":\"wrong_pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("账号或密码不正确"));
    }

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("登录成功");
        body.setData(java.util.Map.of("token", "test-token"));
        when(userAccountService.login("user_a", "123456")).thenReturn(body);

        mockMvc.perform(post("/user/account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user_a\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"))
                .andExpect(jsonPath("$.data.token").value("test-token"));
    }

    @Test
    void register_jsonBody_returnsServiceBody() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("注册成功");
        when(userAccountService.register("new_user", "123456", "123456")).thenReturn(body);

        mockMvc.perform(post("/user/account/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_user\",\"password\":\"123456\",\"confirmedPassword\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("注册成功"));
    }

    @Test
    void register_whenPasswordsDiffer_returnsServiceValidation() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setCode(403);
        body.setMessage("两次输入的密码不一致");
        when(userAccountService.register("new_user", "123456", "654321")).thenReturn(body);

        mockMvc.perform(post("/user/account/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_user\",\"password\":\"123456\",\"confirmedPassword\":\"654321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("两次输入的密码不一致"));
    }

    @Test
    void adminLogin_jsonBody_returnsServiceBody() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("登录成功");
        when(userAccountService.adminLogin("admin", "123456")).thenReturn(body);

        mockMvc.perform(post("/admin/account/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("登录成功"));
    }

    @Test
    void adminPersonalInfo_withAdminToken_returnsServiceBody() throws Exception {
        User admin = user(99, 2, "admin");
        when(userMapper.selectById(99)).thenReturn(admin);
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(userAccountService.adminPersonalInfo()).thenReturn(body);

        mockMvc.perform(get("/admin/personal/info")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(99, "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void logout_withUserToken_returnsServiceBody() throws Exception {
        User user = user(10, 1, "user_a");
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("已退出登录");
        when(userAccountService.logout()).thenReturn(body);

        mockMvc.perform(get("/user/account/logout")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已退出登录"));
    }

    @Test
    void updatePassword_withUserToken_forwardsParameters() throws Exception {
        User user = user(10, 1, "user_a");
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("修改成功");
        when(userAccountService.updatePassword("old_pass", "new_pass")).thenReturn(body);

        mockMvc.perform(post("/user/password/update")
                        .param("pw", "old_pass")
                        .param("npw", "new_pass")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));
    }

    private User user(Integer id, Integer role, String username) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setUsername(username);
        return user;
    }
}
