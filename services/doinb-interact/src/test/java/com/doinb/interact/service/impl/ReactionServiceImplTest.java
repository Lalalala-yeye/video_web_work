package com.doinb.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.VideoDTO;
import com.doinb.interact.client.MessageNotifier;
import com.doinb.interact.client.VideoDirectory;
import com.doinb.interact.mapper.CommentMapper;
import com.doinb.interact.mapper.CommentReactionMapper;
import com.doinb.interact.mapper.VideoReactionMapper;
import com.doinb.interact.pojo.dto.ReactionSummaryDTO;
import com.doinb.interact.pojo.entity.Comment;
import com.doinb.interact.pojo.entity.CommentReaction;
import com.doinb.interact.pojo.entity.VideoReaction;
import com.doinb.interact.support.MybatisPlusLambdaCacheExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 R000 / R001 / R010 */
@ExtendWith(MybatisPlusLambdaCacheExtension.class)
class ReactionServiceImplTest {

    private VideoReactionMapper videoReactionMapper;
    private CommentReactionMapper commentReactionMapper;
    private CommentMapper commentMapper;
    private VideoDirectory videoDirectory;
    private MessageNotifier messageNotifier;
    private ReactionServiceImpl service;

    @BeforeEach
    void setUp() {
        videoReactionMapper = mock(VideoReactionMapper.class);
        commentReactionMapper = mock(CommentReactionMapper.class);
        commentMapper = mock(CommentMapper.class);
        videoDirectory = mock(VideoDirectory.class);
        messageNotifier = mock(MessageNotifier.class);
        service = new ReactionServiceImpl(
                videoReactionMapper, commentReactionMapper, commentMapper, videoDirectory, messageNotifier);
    }

    private VideoDTO video(Integer id, Integer authorId, String title) {
        VideoDTO video = new VideoDTO();
        video.setId(id);
        video.setAuthorId(authorId);
        video.setTitle(title);
        return video;
    }

    @Test
    void setVideoReaction_whenVideoIdNull_returns400() {
        CustomResponse resp = service.setVideoReaction(10, null, 1);

        assertEquals(400, resp.getCode());
        assertEquals("视频 id 不能为空", resp.getMessage());
    }

    @Test
    void setVideoReaction_whenVideoMissing_returns404() {
        when(videoDirectory.findById(12)).thenReturn(null);

        CustomResponse resp = service.setVideoReaction(10, 12, 1);

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
    }

    @Test
    void setVideoReaction_whenInvalid_returns400() {
        when(videoDirectory.findById(12)).thenReturn(video(12, 10, "标题"));

        CustomResponse resp = service.setVideoReaction(10, 12, 9);

        assertEquals(400, resp.getCode());
        assertEquals("reaction 无效（1=赞 -1=踩 0=取消）", resp.getMessage());
    }

    @Test
    void setVideoReaction_whenLike_returns200AndNotifies() {
        when(videoDirectory.findById(12)).thenReturn(video(12, 99, "标题"));
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 1);

        assertEquals(200, resp.getCode());
        assertEquals("操作成功", resp.getMessage());
        ReactionSummaryDTO data = (ReactionSummaryDTO) resp.getData();
        assertEquals(0, data.getLikeCount());
        verify(videoReactionMapper).insert(any(VideoReaction.class));
        verify(messageNotifier).notifyVideoLike(10, 99, 12, "标题");
    }

    @Test
    void setVideoReaction_whenAlreadyLiked_doesNotNotifyTwice() {
        when(videoDirectory.findById(12)).thenReturn(video(12, 99, "标题"));
        VideoReaction existing = new VideoReaction();
        existing.setId(5);
        existing.setUserId(10);
        existing.setVideoId(12);
        existing.setReaction(1);
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 1);

        assertEquals(200, resp.getCode());
        verify(messageNotifier, never()).notifyVideoLike(any(), any(), any(), any());
    }

    @Test
    void setVideoReaction_whenDislike_returns200WithoutNotify() {
        when(videoDirectory.findById(12)).thenReturn(video(12, 99, "标题"));
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, -1);

        assertEquals(200, resp.getCode());
        assertEquals("操作成功", resp.getMessage());
        verify(messageNotifier, never()).notifyVideoLike(any(), any(), any(), any());
    }

    @Test
    void setVideoReaction_whenCancel_deletes() {
        when(videoDirectory.findById(12)).thenReturn(video(12, 99, "标题"));
        VideoReaction existing = new VideoReaction();
        existing.setId(5);
        existing.setUserId(10);
        existing.setVideoId(12);
        existing.setReaction(1);
        when(videoReactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setVideoReaction(10, 12, 0);

        assertEquals(200, resp.getCode());
        verify(videoReactionMapper).deleteById(5);
        verify(messageNotifier, never()).notifyVideoLike(any(), any(), any(), any());
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
        Comment comment = new Comment();
        comment.setId(46);
        comment.setUserId(99);
        comment.setTargetType(1);
        comment.setTargetId(12);
        when(commentMapper.selectById(46)).thenReturn(comment);
        when(commentReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(commentReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setCommentReaction(10, 46, 1);

        assertEquals(200, resp.getCode());
        assertEquals("操作成功", resp.getMessage());
        verify(commentReactionMapper).insert(any(CommentReaction.class));
        verify(messageNotifier).notifyCommentLike(10, 99, 46, "/video/12");
    }

    @Test
    void setCommentReaction_whenCommentOnLive_linkPathNull() {
        Comment comment = new Comment();
        comment.setId(46);
        comment.setUserId(99);
        comment.setTargetType(2);
        comment.setTargetId(3);
        when(commentMapper.selectById(46)).thenReturn(comment);
        when(commentReactionMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(commentReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        service.setCommentReaction(10, 46, 1);

        verify(messageNotifier).notifyCommentLike(10, 99, 46, null);
    }

    @Test
    void getCommentSummaries_whenEmptyIds_returnsEmptyMap() {
        assertEquals(Map.of(), service.getCommentSummaries(List.of(), 10));
    }

    @Test
    void getVideoSummary_countsLikesAndDislikes() {
        VideoReaction like = new VideoReaction();
        like.setUserId(1);
        like.setReaction(1);
        VideoReaction dislike = new VideoReaction();
        dislike.setUserId(2);
        dislike.setReaction(-1);
        when(videoReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(like, dislike));

        ReactionSummaryDTO summary = service.getVideoSummary(12, 2);

        assertEquals(1, summary.getLikeCount());
        assertEquals(1, summary.getDislikeCount());
        assertEquals(-1, summary.getUserReaction());
    }

    @Test
    void setCommentReaction_whenCancel_deletes() {
        Comment comment = new Comment();
        comment.setId(46);
        comment.setUserId(99);
        when(commentMapper.selectById(46)).thenReturn(comment);
        CommentReaction existing = new CommentReaction();
        existing.setId(8);
        existing.setUserId(10);
        existing.setCommentId(46);
        existing.setReaction(1);
        when(commentReactionMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(commentReactionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        CustomResponse resp = service.setCommentReaction(10, 46, 0);

        assertEquals(200, resp.getCode());
        verify(commentReactionMapper).deleteById(8);
        verify(messageNotifier, never()).notifyCommentLike(any(), any(), any(), any());
    }
}
