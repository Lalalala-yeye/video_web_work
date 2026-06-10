package com.doinb.backend.service.reaction.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.CommentReactionMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReactionMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.ReactionSummaryDTO;
import com.doinb.backend.pojo.entity.CommentReaction;
import com.doinb.backend.pojo.entity.VideoReaction;
import com.doinb.backend.service.notification.NotificationService;
import com.doinb.backend.service.reaction.ReactionService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReactionServiceImpl implements ReactionService {

    private static final int LIKE = 1;
    private static final int DISLIKE = -1;

    private final VideoReactionMapper videoReactionMapper;
    private final CommentReactionMapper commentReactionMapper;
    private final VideoMapper videoMapper;
    private final CommentMapper commentMapper;
    private final NotificationService notificationService;

    public ReactionServiceImpl(VideoReactionMapper videoReactionMapper,
                               CommentReactionMapper commentReactionMapper,
                               VideoMapper videoMapper,
                               CommentMapper commentMapper,
                               NotificationService notificationService) {
        this.videoReactionMapper = videoReactionMapper;
        this.commentReactionMapper = commentReactionMapper;
        this.videoMapper = videoMapper;
        this.commentMapper = commentMapper;
        this.notificationService = notificationService;
    }

    @Override
    public ReactionSummaryDTO getVideoSummary(Integer videoId, Integer viewerUserId) {
        List<VideoReaction> rows = videoReactionMapper.selectList(
                new LambdaQueryWrapper<VideoReaction>().eq(VideoReaction::getVideoId, videoId));
        return toVideoSummary(rows, viewerUserId);
    }

    @Override
    public CustomResponse setVideoReaction(Integer userId, Integer videoId, Integer reaction) {
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        if (videoMapper.selectById(videoId) == null) {
            return fail(404, "视频不存在");
        }
        if (!isValidReaction(reaction)) {
            return fail(400, "reaction 无效（1=赞 -1=踩 0=取消）");
        }

        VideoReaction existing = videoReactionMapper.selectOne(new LambdaQueryWrapper<VideoReaction>()
                .eq(VideoReaction::getUserId, userId)
                .eq(VideoReaction::getVideoId, videoId));

        if (reaction == 0) {
            if (existing != null) {
                videoReactionMapper.deleteById(existing.getId());
            }
        } else if (existing == null) {
            VideoReaction row = new VideoReaction();
            row.setUserId(userId);
            row.setVideoId(videoId);
            row.setReaction(reaction);
            videoReactionMapper.insert(row);
        } else {
            existing.setReaction(reaction);
            videoReactionMapper.updateById(existing);
        }

        maybeNotifyVideoLike(userId, videoId, reaction, existing);

        CustomResponse resp = ok("操作成功");
        resp.setData(getVideoSummary(videoId, userId));
        return resp;
    }

    @Override
    public ReactionSummaryDTO getCommentSummary(Integer commentId, Integer viewerUserId) {
        List<CommentReaction> rows = commentReactionMapper.selectList(
                new LambdaQueryWrapper<CommentReaction>().eq(CommentReaction::getCommentId, commentId));
        return toCommentSummary(rows, viewerUserId);
    }

    @Override
    public Map<Integer, ReactionSummaryDTO> getCommentSummaries(List<Integer> commentIds, Integer viewerUserId) {
        Map<Integer, ReactionSummaryDTO> map = new HashMap<>();
        if (commentIds == null || commentIds.isEmpty()) {
            return map;
        }
        List<CommentReaction> rows = commentReactionMapper.selectList(
                new LambdaQueryWrapper<CommentReaction>().in(CommentReaction::getCommentId, commentIds));
        Map<Integer, java.util.ArrayList<CommentReaction>> grouped = new HashMap<>();
        for (CommentReaction row : rows) {
            grouped.computeIfAbsent(row.getCommentId(), k -> new java.util.ArrayList<>()).add(row);
        }
        for (Integer commentId : commentIds) {
            map.put(commentId, toCommentSummary(grouped.getOrDefault(commentId, new java.util.ArrayList<>()), viewerUserId));
        }
        return map;
    }

    @Override
    public CustomResponse setCommentReaction(Integer userId, Integer commentId, Integer reaction) {
        if (commentId == null) {
            return fail(400, "评论 id 不能为空");
        }
        if (commentMapper.selectById(commentId) == null) {
            return fail(404, "评论不存在");
        }
        if (!isValidReaction(reaction)) {
            return fail(400, "reaction 无效（1=赞 -1=踩 0=取消）");
        }

        CommentReaction existing = commentReactionMapper.selectOne(new LambdaQueryWrapper<CommentReaction>()
                .eq(CommentReaction::getUserId, userId)
                .eq(CommentReaction::getCommentId, commentId));

        if (reaction == 0) {
            if (existing != null) {
                commentReactionMapper.deleteById(existing.getId());
            }
        } else if (existing == null) {
            CommentReaction row = new CommentReaction();
            row.setUserId(userId);
            row.setCommentId(commentId);
            row.setReaction(reaction);
            commentReactionMapper.insert(row);
        } else {
            existing.setReaction(reaction);
            commentReactionMapper.updateById(existing);
        }

        maybeNotifyCommentLike(userId, commentId, reaction, existing);

        CustomResponse resp = ok("操作成功");
        resp.setData(getCommentSummary(commentId, userId));
        return resp;
    }

    private void maybeNotifyVideoLike(Integer userId, Integer videoId, Integer reaction, VideoReaction existing) {
        if (!Objects.equals(reaction, LIKE)) {
            return;
        }
        if (existing != null && Objects.equals(existing.getReaction(), LIKE)) {
            return;
        }
        notificationService.notifyVideoLike(userId, videoId);
    }

    private void maybeNotifyCommentLike(Integer userId, Integer commentId, Integer reaction, CommentReaction existing) {
        if (!Objects.equals(reaction, LIKE)) {
            return;
        }
        if (existing != null && Objects.equals(existing.getReaction(), LIKE)) {
            return;
        }
        notificationService.notifyCommentLike(userId, commentId);
    }

    private boolean isValidReaction(Integer reaction) {
        return reaction != null && (reaction == 0 || reaction == LIKE || reaction == DISLIKE);
    }

    private ReactionSummaryDTO toVideoSummary(List<VideoReaction> rows, Integer viewerUserId) {
        long likes = 0;
        long dislikes = 0;
        int userReaction = 0;
        for (VideoReaction row : rows) {
            if (Objects.equals(row.getReaction(), LIKE)) {
                likes++;
            } else if (Objects.equals(row.getReaction(), DISLIKE)) {
                dislikes++;
            }
            if (viewerUserId != null && Objects.equals(row.getUserId(), viewerUserId)) {
                userReaction = row.getReaction() != null ? row.getReaction() : 0;
            }
        }
        return buildSummary(likes, dislikes, userReaction);
    }

    private ReactionSummaryDTO toCommentSummary(List<CommentReaction> rows, Integer viewerUserId) {
        long likes = 0;
        long dislikes = 0;
        int userReaction = 0;
        for (CommentReaction row : rows) {
            if (Objects.equals(row.getReaction(), LIKE)) {
                likes++;
            } else if (Objects.equals(row.getReaction(), DISLIKE)) {
                dislikes++;
            }
            if (viewerUserId != null && Objects.equals(row.getUserId(), viewerUserId)) {
                userReaction = row.getReaction() != null ? row.getReaction() : 0;
            }
        }
        return buildSummary(likes, dislikes, userReaction);
    }

    private ReactionSummaryDTO buildSummary(long likes, long dislikes, int userReaction) {
        ReactionSummaryDTO dto = new ReactionSummaryDTO();
        dto.setLikeCount(likes);
        dto.setDislikeCount(dislikes);
        dto.setUserReaction(userReaction);
        return dto;
    }

    private CustomResponse ok(String message) {
        CustomResponse resp = new CustomResponse();
        resp.setMessage(message);
        return resp;
    }

    private CustomResponse fail(int code, String message) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}
