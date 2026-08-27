package com.doinb.backend.api;

import com.doinb.backend.controller.ReactionController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.ReactionSummaryDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.reaction.ReactionService;
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

/** 点赞/点踩接口的公开查询与登录写操作测试。 */
@WebMvcTest(controllers = ReactionController.class)
@ImportApiSecurity
class ReactionApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private ReactionService reactionService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void summary_withoutToken_returnsCounts() throws Exception {
        ReactionSummaryDTO summary = new ReactionSummaryDTO();
        summary.setLikeCount(3);
        summary.setDislikeCount(1);
        summary.setUserReaction(0);
        when(reactionService.getVideoSummary(12, null)).thenReturn(summary);

        mockMvc.perform(get("/video/reaction/summary").param("videoId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.likeCount").value(3))
                .andExpect(jsonPath("$.data.dislikeCount").value(1))
                .andExpect(jsonPath("$.data.userReaction").value(0));

        verify(reactionService).getVideoSummary(12, null);
    }

    @Test
    void videoReaction_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(post("/video/reaction")
                        .param("videoId", "12")
                        .param("reaction", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void videoReaction_withToken_forwardsCurrentUser() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("操作成功");
        when(reactionService.setVideoReaction(10, 12, 1)).thenReturn(body);

        mockMvc.perform(post("/video/reaction")
                        .param("videoId", "12")
                        .param("reaction", "1")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(reactionService).setVideoReaction(10, 12, 1);
    }

    @Test
    void commentReaction_withToken_forwardsCurrentUser() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("操作成功");
        when(reactionService.setCommentReaction(10, 50, 1)).thenReturn(body);

        mockMvc.perform(post("/comment/reaction")
                        .param("commentId", "50")
                        .param("reaction", "1")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(reactionService).setCommentReaction(10, 50, 1);
    }

    @Test
    void videoReaction_whenValueInvalid_returnsServiceValidation() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setCode(400);
        body.setMessage("reaction 参数非法");
        when(reactionService.setVideoReaction(10, 12, 2)).thenReturn(body);

        mockMvc.perform(post("/video/reaction")
                        .param("videoId", "12")
                        .param("reaction", "2")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("reaction 参数非法"));

        verify(reactionService).setVideoReaction(10, 12, 2);
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(1);
        return user;
    }
}
