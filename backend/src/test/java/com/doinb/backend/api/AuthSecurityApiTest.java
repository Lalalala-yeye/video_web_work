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
}
