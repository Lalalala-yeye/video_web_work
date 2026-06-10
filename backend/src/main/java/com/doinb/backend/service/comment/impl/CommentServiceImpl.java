package com.doinb.backend.service.comment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.CommentDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.comment.CommentService;
import com.doinb.backend.service.reaction.ReactionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private static final int TARGET_VIDEO = 1;
    private static final int TARGET_LIVE = 2;

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final ReactionService reactionService;

    public CommentServiceImpl(CommentMapper commentMapper,
                              UserMapper userMapper,
                              VideoMapper videoMapper,
                              LiveRoomMapper liveRoomMapper,
                              ReactionService reactionService) {
        this.commentMapper = commentMapper;
        this.userMapper = userMapper;
        this.videoMapper = videoMapper;
        this.liveRoomMapper = liveRoomMapper;
        this.reactionService = reactionService;
    }

    @Override
    public CustomResponse add(Integer userId, Integer targetId, Integer targetType, String content) {
        if (targetId == null || targetType == null) {
            return fail(400, "评论目标无效");
        }
        if (targetType != TARGET_VIDEO && targetType != TARGET_LIVE) {
            return fail(400, "targetType 无效（1=视频 2=直播间）");
        }
        if (!StringUtils.hasText(content)) {
            return fail(400, "评论内容不能为空");
        }
        if (content.length() > 500) {
            return fail(400, "评论内容不能超过500字");
        }

        if (targetType == TARGET_VIDEO && videoMapper.selectById(targetId) == null) {
            return fail(404, "视频不存在");
        }
        if (targetType == TARGET_LIVE && liveRoomMapper.selectById(targetId) == null) {
            return fail(404, "直播间不存在");
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetId(targetId);
        comment.setTargetType(targetType);
        comment.setContent(content.trim());
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        CustomResponse resp = ok("评论成功");
        User user = userMapper.selectById(userId);
        resp.setData(toDTO(comment, user));
        return resp;
    }

    @Override
    public PageResult<CommentDTO> listByTarget(Integer targetId, Integer targetType, long page, long size,
                                               Integer viewerUserId) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Comment> mpPage = new Page<>(safePage, safeSize);
        commentMapper.selectPage(mpPage, new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetId, targetId)
                .eq(Comment::getTargetType, targetType)
                .orderByDesc(Comment::getCreateTime));

        List<Comment> records = mpPage.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(mpPage.getTotal(), safePage, safeSize, List.of());
        }

        List<Integer> userIds = records.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Integer> commentIds = records.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Integer, com.doinb.backend.pojo.dto.ReactionSummaryDTO> reactionMap =
                reactionService.getCommentSummaries(commentIds, viewerUserId);

        List<CommentDTO> list = new ArrayList<>();
        for (Comment comment : records) {
            CommentDTO dto = toDTO(comment, userMap.get(comment.getUserId()));
            dto.setReactions(reactionMap.get(comment.getId()));
            list.add(dto);
        }
        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, list);
    }

    private CommentDTO toDTO(Comment comment, User user) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setUserId(comment.getUserId());
        dto.setUserNickname(user != null ? user.getNickname() : "匿名用户");
        dto.setUserAvatar(user != null ? user.getAvatar() : null);
        dto.setTargetId(comment.getTargetId());
        dto.setTargetType(comment.getTargetType());
        dto.setContent(comment.getContent());
        dto.setCreateTime(comment.getCreateTime());
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
