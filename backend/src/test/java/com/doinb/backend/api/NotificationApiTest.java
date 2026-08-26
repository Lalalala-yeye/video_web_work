package com.doinb.backend.api;

import com.doinb.backend.controller.NotificationController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.NotificationDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.notification.NotificationService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 通知接口的基础鉴权与未读数量测试。 */
@WebMvcTest(controllers = NotificationController.class)
@ImportApiSecurity
class NotificationApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private NotificationService notificationService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void list_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(get("/notification/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_withToken_returnsAtLeastOneNotification() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        NotificationDTO notification = new NotificationDTO();
        notification.setId(40);
        notification.setPreview("你的视频收到了点赞");
        notification.setIsRead(false);
        when(notificationService.list(10, 1, 20))
                .thenReturn(new PageResult<>(1, 1, 20, List.of(notification)));

        mockMvc.perform(get("/notification/list")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records.length()").value(1))
                .andExpect(jsonPath("$.data.records[0].preview").value("你的视频收到了点赞"));

        verify(notificationService).list(10, 1, 20);
    }

    @Test
    void unreadCount_withToken_returnsCount() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        when(notificationService.countUnread(10)).thenReturn(5L);

        mockMvc.perform(get("/notification/unread-count")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.count").value(5));

        verify(notificationService).countUnread(10);
    }

    @Test
    void markRead_withToken_forwardsNotificationId() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("已读");
        when(notificationService.markRead(10, 40)).thenReturn(body);

        mockMvc.perform(post("/notification/read")
                        .param("id", "40")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已读"));

        verify(notificationService).markRead(10, 40);
    }

    @Test
    void markRead_withoutId_forwardsNullForReadAll() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("全部已读");
        when(notificationService.markRead(10, null)).thenReturn(body);

        mockMvc.perform(post("/notification/read")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("全部已读"));

        verify(notificationService).markRead(10, null);
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(1);
        return user;
    }
}
