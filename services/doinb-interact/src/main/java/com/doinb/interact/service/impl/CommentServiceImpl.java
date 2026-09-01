package com.doinb.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.common.dto.UserDTO;
import com.doinb.interact.client.LiveDirectory;
import com.doinb.interact.client.UserDirectory;
import com.doinb.interact.client.VideoDirectory;
import com.doinb.interact.mapper.CommentMapper;
import com.doinb.interact.pojo.dto.CommentDTO;
import com.doinb.interact.pojo.dto.ReactionSummaryDTO;
import com.doinb.interact.pojo.entity.Comment;
import com.doinb.interact.service.CommentService;
import com.doinb.interact.service.ReactionService;
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
    private final ReactionService reactionService;
    private final VideoDirectory videoDirectory;
    private final LiveDirectory liveDirectory;
    private final UserDirectory userDirectory;

    public CommentServiceImpl(CommentMapper commentMapper,
                              ReactionService reactionService,
                              VideoDirectory videoDirectory,
                              LiveDirectory liveDirectory,
                              UserDirectory userDirectory) {
        this.commentMapper = commentMapper;
        this.reactionService = reactionService;
        this.videoDirectory = videoDirectory;
        this.liveDirectory = liveDirectory;
        this.userDirectory = userDirectory;
    }

    @Override
    public CustomResponse add(Integer userId, Integer targetId, Integer targetType, String content) {
        if (targetId == null || targetType == null) {
            return CustomResponse.fail(400, "评论目标无效");
        }
        if (targetType != TARGET_VIDEO && targetType != TARGET_LIVE) {
            return CustomResponse.fail(400, "targetType 无效（1=视频 2=直播间）");
        }
        if (!StringUtils.hasText(content)) {
            return CustomResponse.fail(400, "评论内容不能为空");
        }
        if (content.length() > 500) {
            return CustomResponse.fail(400, "评论内容不能超过500字");
        }

        if (targetType == TARGET_VIDEO && videoDirectory.findById(targetId) == null) {
            return CustomResponse.fail(404, "视频不存在");
        }
        if (targetType == TARGET_LIVE) {
            LiveRoomDTO room = liveDirectory.findById(targetId);
            if (room == null) {
                return CustomResponse.fail(404, "直播间不存在");
            }
            if (!Boolean.TRUE.equals(room.getIsLive())) {
                return CustomResponse.fail(400, "直播间未开播，无法发送弹幕");
            }
        }

        Comment comment = new Comment();
        comment.setUserId(userId);
        comment.setTargetId(targetId);
        comment.setTargetType(targetType);
        comment.setContent(content.trim());
        comment.setCreateTime(LocalDateTime.now());
        commentMapper.insert(comment);

        return CustomResponse.ok("评论成功", toDTO(comment, userDirectory.findById(userId)));
    }

    @Override
    public PageResult<CommentDTO> listByTarget(Integer targetId, Integer targetType, long page, long size,
                                               Integer viewerUserId) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Comment> mpPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetId, targetId)
                .eq(Comment::getTargetType, targetType)
                .orderByAsc(Comment::getCreateTime);

        if (targetType == TARGET_LIVE) {
            LiveRoomDTO room = liveDirectory.findById(targetId);
            if (room == null || !Boolean.TRUE.equals(room.getIsLive()) || room.getSessionStart() == null) {
                return new PageResult<>(0, safePage, safeSize, List.of());
            }
            wrapper.ge(Comment::getCreateTime, room.getSessionStart());
        }

        commentMapper.selectPage(mpPage, wrapper);

        List<Comment> records = mpPage.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(mpPage.getTotal(), safePage, safeSize, List.of());
        }

        List<Integer> userIds = records.stream()
                .map(Comment::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, UserDTO> userMap = userDirectory.findByIds(userIds);

        List<Integer> commentIds = records.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Integer, ReactionSummaryDTO> reactionMap =
                reactionService.getCommentSummaries(commentIds, viewerUserId);

        List<CommentDTO> list = new ArrayList<>();
        for (Comment comment : records) {
            CommentDTO dto = toDTO(comment, userMap.get(comment.getUserId()));
            dto.setReactions(reactionMap.get(comment.getId()));
            list.add(dto);
        }
        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, list);
    }

    private CommentDTO toDTO(Comment comment, UserDTO user) {
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
}
