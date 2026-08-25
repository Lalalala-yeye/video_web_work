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
import com.doinb.backend.pojo.entity.VideoReaction;
import com.doinb.backend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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

    @Test
    void setVideoReaction_whenVideoIdNull_returns400() {
        CustomResponse resp = service.setVideoReaction(10, null, 1);

        assertEquals(400, resp.getCode());
        assertEquals("视频 id 不能为空", resp.getMessage());
    }

    @Test
    void setVideoReaction_whenCancelNoExisting_returns200() {
        when(videoMapper.selectById(12)).thenReturn(new Video());
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 0);

        assertEquals(200, resp.getCode());
        verify(videoReactionMapper, never()).insert(any(VideoReaction.class));
        verify(videoReactionMapper, never()).deleteById(any());
    }

    @Test
    void setVideoReaction_whenCancelExisting_deletes() {
        when(videoMapper.selectById(12)).thenReturn(new Video());
        VideoReaction existing = new VideoReaction();
        existing.setId(5);
        existing.setReaction(1);
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 0);

        assertEquals(200, resp.getCode());
        verify(videoReactionMapper).deleteById(5);
    }

    @Test
    void setVideoReaction_whenAlreadyLike_doesNotNotify() {
        when(videoMapper.selectById(12)).thenReturn(new Video());
        VideoReaction existing = new VideoReaction();
        existing.setId(5);
        existing.setUserId(10);
        existing.setReaction(1);
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 1);

        assertEquals(200, resp.getCode());
        verify(videoReactionMapper).updateById(existing);
        verify(notificationService, never()).notifyVideoLike(any(), any());
    }

    @Test
    void setCommentReaction_whenCommentIdNull_returns400() {
        CustomResponse resp = service.setCommentReaction(10, null, 1);

        assertEquals(400, resp.getCode());
        assertEquals("评论 id 不能为空", resp.getMessage());
    }

    @Test
    void setCommentReaction_whenInvalid_returns400() {
        when(commentMapper.selectById(46)).thenReturn(new Comment());

        CustomResponse resp = service.setCommentReaction(10, 46, 9);

        assertEquals(400, resp.getCode());
        assertEquals("reaction 无效（1=赞 -1=踩 0=取消）", resp.getMessage());
    }

    @Test
    void setCommentReaction_whenCancelExisting_deletes() {
        when(commentMapper.selectById(46)).thenReturn(new Comment());
        CommentReaction existing = new CommentReaction();
        existing.setId(8);
        existing.setReaction(1);
        when(commentReactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(commentReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setCommentReaction(10, 46, 0);

        assertEquals(200, resp.getCode());
        verify(commentReactionMapper).deleteById(8);
    }

    @Test
    void getVideoSummary_countsLikesAndUserReaction() {
        VideoReaction like = new VideoReaction();
        like.setUserId(10);
        like.setReaction(1);
        VideoReaction dislike = new VideoReaction();
        dislike.setUserId(11);
        dislike.setReaction(-1);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(like, dislike));

        ReactionSummaryDTO summary = service.getVideoSummary(12, 10);

        assertEquals(1, summary.getLikeCount());
        assertEquals(1, summary.getDislikeCount());
        assertEquals(1, summary.getUserReaction());
    }

    @Test
    void getCommentSummaries_whenNull_returnsEmpty() {
        Map<Integer, ReactionSummaryDTO> map = service.getCommentSummaries(null, 10);

        assertTrue(map.isEmpty());
    }

    @Test
    void getCommentSummaries_groupsByComment() {
        CommentReaction r1 = new CommentReaction();
        r1.setCommentId(1);
        r1.setUserId(10);
        r1.setReaction(1);
        when(commentReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(r1));

        Map<Integer, ReactionSummaryDTO> map = service.getCommentSummaries(List.of(1, 2), 10);

        assertEquals(2, map.size());
        assertEquals(1, map.get(1).getLikeCount());
        assertEquals(0, map.get(2).getLikeCount());
    }
}
