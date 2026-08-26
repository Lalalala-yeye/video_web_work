package com.doinb.backend.api;

import com.doinb.backend.controller.SubscriptionController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.FeedItemDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.subscription.SubscriptionService;
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

/** 关注接口的基础鉴权与状态查询测试。 */
@WebMvcTest(controllers = SubscriptionController.class)
@ImportApiSecurity
class SubscriptionApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private SubscriptionService subscriptionService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void follow_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(post("/subscription/follow").param("targetId", "20"))
                .andExpect(status().isForbidden());
    }

    @Test
    void follow_withToken_returnsSuccess() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("关注成功");
        when(subscriptionService.follow(10, 20)).thenReturn(body);

        mockMvc.perform(post("/subscription/follow")
                        .param("targetId", "20")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("关注成功"));

        verify(subscriptionService).follow(10, 20);
    }

    @Test
    void status_withToken_returnsFollowingState() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        when(subscriptionService.isFollowing(10, 20)).thenReturn(true);

        mockMvc.perform(get("/subscription/status")
                        .param("targetId", "20")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.following").value(true));

        verify(subscriptionService).isFollowing(10, 20);
    }

    @Test
    void unfollow_withToken_forwardsCurrentUser() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("已取消关注");
        when(subscriptionService.unfollow(10, 20)).thenReturn(body);

        mockMvc.perform(post("/subscription/unfollow")
                        .param("targetId", "20")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已取消关注"));

        verify(subscriptionService).unfollow(10, 20);
    }

    @Test
    void following_withToken_returnsDefaultPage() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        when(subscriptionService.listFollowing(10, 1, 12))
                .thenReturn(new PageResult<UserDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/subscription/following")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(subscriptionService).listFollowing(10, 1, 12);
    }

    @Test
    void feed_withToken_returnsDefaultPage() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        when(subscriptionService.feed(10, 1, 12))
                .thenReturn(new PageResult<FeedItemDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/subscription/feed")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(subscriptionService).feed(10, 1, 12);
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(1);
        return user;
    }
}
