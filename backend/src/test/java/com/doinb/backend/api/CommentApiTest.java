package com.doinb.backend.api;

import com.doinb.backend.controller.CommentController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.comment.CommentService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 对应测试报告 C001（未登录 HTTP 403）/ C000（带 Token 进入 Controller） */
@WebMvcTest(controllers = CommentController.class)
@ImportApiSecurity
class CommentApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private CommentService commentService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void add_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(post("/comment/add")
                        .param("targetId", "12")
                        .param("targetType", "1")
                        .param("content", "测试评论"))
                .andExpect(status().isForbidden());
    }

    @Test
    void add_withToken_returnsServiceBody() throws Exception {
        User user = new User();
        user.setId(10);
        user.setRole(1);
        when(userMapper.selectById(10)).thenReturn(user);

        CustomResponse body = new CustomResponse();
        body.setMessage("评论成功");
        when(commentService.add(10, 12, 1, "测试评论")).thenReturn(body);

        String token = jwtUtil.createToken(10, "user");
        mockMvc.perform(post("/comment/add")
                        .param("targetId", "12")
                        .param("targetType", "1")
                        .param("content", "测试评论")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("评论成功"));
    }
}
