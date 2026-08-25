package com.doinb.backend.service.reaction.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.CommentReactionMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReactionMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.ReactionSummaryDTO;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.CommentReaction;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 R000 / R001 / R010 */
class ReactionServiceImplTest {

    private VideoReactionMapper videoReactionMapper;
    private CommentReactionMapper commentReactionMapper;
    private VideoMapper videoMapper;
    private CommentMapper commentMapper;
    private NotificationService notificationService;
    private ReactionServiceImpl service;

    @BeforeEach
    void setUp() {
        videoReactionMapper = mock(VideoReactionMapper.class);
        commentReactionMapper = mock(CommentReactionMapper.class);
        videoMapper = mock(VideoMapper.class);
        commentMapper = mock(CommentMapper.class);
        notificationService = mock(NotificationService.class);
        service = new ReactionServiceImpl(
                videoReactionMapper,
                commentReactionMapper,
                videoMapper,
                commentMapper,
                notificationService
        );
    }

    @Test
    void setVideoReaction_whenVideoMissing_returns404() {
        when(videoMapper.selectById(12)).thenReturn(null);

        CustomResponse resp = service.setVideoReaction(10, 12, 1);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void setVideoReaction_whenInvalid_returns400() {
        when(videoMapper.selectById(12)).thenReturn(new Video());

        CustomResponse resp = service.setVideoReaction(10, 12, 9);

        assertEquals(400, resp.getCode());
        assertEquals("reaction 无效（1=赞 -1=踩 0=取消）", resp.getMessage());
    }

    @Test
    void setVideoReaction_whenLike_returns200AndNotifies() {
        when(videoMapper.selectById(12)).thenReturn(new Video());
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 1);

        assertEquals(200, resp.getCode());
        assertEquals("操作成功", resp.getMessage());
        ReactionSummaryDTO data = (ReactionSummaryDTO) resp.getData();
        assertEquals(0, data.getLikeCount());
        verify(notificationService).notifyVideoLike(10, 12);
    }

    @Test
    void setVideoReaction_whenDislike_returns200() {
        when(videoMapper.selectById(12)).thenReturn(new Video());
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, -1);

        assertEquals(200, resp.getCode());
        assertEquals("操作成功", resp.getMessage());
    }

    @Test
    void setCommentReaction_whenCommentMissing_returns404() {
        when(commentMapper.selectById(46)).thenReturn(null);

        CustomResponse resp = service.setCommentReaction(10, 46, 1);

        assertEquals(404, resp.getCode());
        assertEquals("评论不存在", resp.getMessage());
    }

    @Test
    void setCommentReaction_whenLike_returns200AndNotifies() {
        when(commentMapper.selectById(46)).thenReturn(new Comment());
        when(commentReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(commentReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setCommentReaction(10, 46, 1);

        assertEquals(200, resp.getCode());
        assertEquals("操作成功", resp.getMessage());
        verify(commentReactionMapper).insert(any(CommentReaction.class));
        verify(notificationService).notifyCommentLike(10, 46);
    }
}
