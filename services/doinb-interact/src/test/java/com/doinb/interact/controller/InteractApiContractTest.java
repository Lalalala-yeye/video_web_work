package com.doinb.interact.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.GatewayHeaders;
import com.doinb.common.PageResult;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.web.DownstreamAuthFilter;
import com.doinb.interact.pojo.dto.CommentDTO;
import com.doinb.interact.pojo.dto.FeedItemDTO;
import com.doinb.interact.pojo.dto.ReactionSummaryDTO;
import com.doinb.interact.service.CommentService;
import com.doinb.interact.service.ReactionService;
import com.doinb.interact.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/** 对外路径与参数契约（服务接口清单 §4），并校验网关用户头透传。 */
class InteractApiContractTest {

    private CommentService commentService;
    private ReactionService reactionService;
    private SubscriptionService subscriptionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        commentService = mock(CommentService.class);
        reactionService = mock(ReactionService.class);
        subscriptionService = mock(SubscriptionService.class);
        DoinbProperties properties = new DoinbProperties();
        properties.setRole("service");
        properties.setInternalToken("test-internal-token");
        properties.setPublicPathPrefixes(List.of("/health", "/comment/list", "/video/reaction/summary"));
        mockMvc = standaloneSetup(
                new CommentController(commentService),
                new ReactionController(reactionService),
                new SubscriptionController(subscriptionService))
                .addFilters(new DownstreamAuthFilter(properties))
                .build();
    }

    @Test
    void protectedEndpoints_withoutUserHeader_return403() throws Exception {
        mockMvc.perform(post("/comment/add")
                        .param("targetId", "12").param("targetType", "1").param("content", "你好"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/video/reaction")
                        .param("videoId", "12").param("reaction", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/comment/reaction")
                        .param("commentId", "46").param("reaction", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/subscription/follow").param("targetId", "11"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/subscription/status").param("targetId", "11"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/subscription/feed"))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicEndpoints_withoutUserHeader_return200() throws Exception {
        when(commentService.listByTarget(eq(12), eq(1), eq(1L), eq(20L), any()))
                .thenReturn(new PageResult<>(0, 1, 20, List.of()));
        when(reactionService.getVideoSummary(12, null))
                .thenReturn(new ReactionSummaryDTO());

        mockMvc.perform(get("/comment/list")
                        .param("targetId", "12").param("targetType", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(0));
        mockMvc.perform(get("/video/reaction/summary").param("videoId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void commentEndpoints_forwardGatewayUserAndKeepPaths() throws Exception {
        when(commentService.add(10, 12, 1, "你好")).thenReturn(CustomResponse.ok("评论成功", null));

        mockMvc.perform(post("/comment/add")
                        .header(GatewayHeaders.USER_ID, "10")
                        .param("targetId", "12").param("targetType", "1").param("content", "你好"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("评论成功"));

        verify(commentService).add(10, 12, 1, "你好");
    }

    @Test
    void reactionEndpoints_forwardGatewayUserAndKeepPaths() throws Exception {
        when(reactionService.setVideoReaction(10, 12, 1)).thenReturn(CustomResponse.ok("操作成功", null));
        when(reactionService.setCommentReaction(10, 46, -1)).thenReturn(CustomResponse.ok("操作成功", null));

        mockMvc.perform(post("/video/reaction")
                        .header(GatewayHeaders.USER_ID, "10")
                        .param("videoId", "12").param("reaction", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("操作成功"));
        mockMvc.perform(post("/comment/reaction")
                        .header(GatewayHeaders.USER_ID, "10")
                        .param("commentId", "46").param("reaction", "-1"))
                .andExpect(status().isOk());

        verify(reactionService).setVideoReaction(10, 12, 1);
        verify(reactionService).setCommentReaction(10, 46, -1);
    }

    @Test
    void subscriptionEndpoints_forwardGatewayUserAndKeepPaths() throws Exception {
        when(subscriptionService.follow(10, 11)).thenReturn(CustomResponse.ok("关注成功", null));
        when(subscriptionService.unfollow(10, 11)).thenReturn(CustomResponse.ok("已取消关注", null));
        when(subscriptionService.isFollowing(10, 11)).thenReturn(true);
        when(subscriptionService.listFollowing(10, 1, 12))
                .thenReturn(new PageResult<>(1, 1, 12, List.of(new UserDTO())));
        when(subscriptionService.feed(10, 1, 12))
                .thenReturn(new PageResult<>(0, 1, 12, List.of()));

        mockMvc.perform(post("/subscription/follow")
                        .header(GatewayHeaders.USER_ID, "10").param("targetId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("关注成功"));
        mockMvc.perform(post("/subscription/unfollow")
                        .header(GatewayHeaders.USER_ID, "10").param("targetId", "11"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/subscription/status")
                        .header(GatewayHeaders.USER_ID, "10").param("targetId", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.following").value(true));
        mockMvc.perform(get("/subscription/following").header(GatewayHeaders.USER_ID, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
        mockMvc.perform(get("/subscription/feed").header(GatewayHeaders.USER_ID, "10"))
                .andExpect(status().isOk());

        verify(subscriptionService).follow(10, 11);
        verify(subscriptionService).unfollow(10, 11);
        verify(subscriptionService).isFollowing(10, 11);
        verify(subscriptionService).listFollowing(10, 1, 12);
        verify(subscriptionService).feed(10, 1, 12);
    }
}
