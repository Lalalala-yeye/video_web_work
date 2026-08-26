package com.doinb.backend.api;

import com.doinb.backend.controller.MessageController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.message.MessageService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 私信接口的基础鉴权与当前用户转发测试。 */
@WebMvcTest(controllers = MessageController.class)
@ImportApiSecurity
class MessageApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private MessageService messageService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void send_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(post("/message/send")
                        .param("roomId", "30")
                        .param("content", "你好"))
                .andExpect(status().isForbidden());
    }

    @Test
    void send_withToken_returnsMessageContent() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("发送成功");
        body.setData(java.util.Map.of("content", "你好，对方！"));
        when(messageService.send(10, 30, "你好，对方！")).thenReturn(body);

        mockMvc.perform(post("/message/send")
                        .param("roomId", "30")
                        .param("content", "你好，对方！")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("发送成功"))
                .andExpect(jsonPath("$.data.content").value("你好，对方！"));

        verify(messageService).send(10, 30, "你好，对方！");
    }

    @Test
    void openRoom_withToken_forwardsCurrentUser() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(messageService.openRoom(10, 20)).thenReturn(body);

        mockMvc.perform(post("/message/room/open")
                        .param("peerId", "20")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(messageService).openRoom(10, 20);
    }

    @Test
    void getRoom_withToken_forwardsPagingParameters() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(messageService.getRoom(10, 30, 1, 50)).thenReturn(body);

        mockMvc.perform(get("/message/room/get")
                        .param("roomId", "30")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(messageService).getRoom(10, 30, 1, 50);
    }

    @Test
    void send_whenContentBlank_returnsServiceValidation() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setCode(400);
        body.setMessage("消息内容不能为空");
        when(messageService.send(10, 30, "")).thenReturn(body);

        mockMvc.perform(post("/message/send")
                        .param("roomId", "30")
                        .param("content", "")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("消息内容不能为空"));

        verify(messageService).send(10, 30, "");
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(1);
        return user;
    }
}
