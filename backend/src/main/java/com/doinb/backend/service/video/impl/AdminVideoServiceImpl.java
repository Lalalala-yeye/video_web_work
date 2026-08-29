package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.CommentReactionMapper;
import com.doinb.backend.mapper.PlayHistoryMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.mapper.VideoReactionMapper;
import com.doinb.backend.mapper.VideoReportMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.VideoStatus;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.dto.VideoReportDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.pojo.entity.VideoReport;
import com.doinb.backend.pojo.entity.PlayHistory;
import com.doinb.backend.pojo.entity.VideoReaction;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.CommentReaction;
import com.doinb.backend.service.notification.NotificationService;
import com.doinb.backend.service.video.AdminVideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminVideoServiceImpl implements AdminVideoService {

    private static final int ROLE_ADMIN = 2;

    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    private final VideoReportMapper videoReportMapper;
    private final PlayHistoryMapper playHistoryMapper;
    private final VideoReactionMapper videoReactionMapper;
    private final CommentMapper commentMapper;
    private final CommentReactionMapper commentReactionMapper;
    private final NotificationService notificationService;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public AdminVideoServiceImpl(VideoMapper videoMapper,
                                 UserMapper userMapper,
                                 VideoReportMapper videoReportMapper,
                                 PlayHistoryMapper playHistoryMapper,
                                 VideoReactionMapper videoReactionMapper,
                                 CommentMapper commentMapper,
                                 CommentReactionMapper commentReactionMapper,
                                 NotificationService notificationService) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
        this.videoReportMapper = videoReportMapper;
        this.playHistoryMapper = playHistoryMapper;
        this.videoReactionMapper = videoReactionMapper;
        this.commentMapper = commentMapper;
        this.commentReactionMapper = commentReactionMapper;
        this.notificationService = notificationService;
    }

    @Override
    public PageResult<VideoDTO> listPending(Integer adminRole, long page, long size) {
        requireAdmin(adminRole);
        return listByStatus(VideoStatus.PENDING, page, size);
    }

    @Override
    public PageResult<VideoDTO> listReportReview(Integer adminRole, long page, long size) {
        requireAdmin(adminRole);
        return listByStatus(VideoStatus.REPORT_REVIEW, page, size);
    }

    @Override
    public CustomResponse getVideoForPreview(Integer adminRole, Integer videoId) {
        if (adminRole == null || adminRole != ROLE_ADMIN) {
            return fail(403, "需要管理员权限");
        }
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        User author = userMapper.selectById(video.getAuthorId());
        CustomResponse resp = ok("OK");
        resp.setData(toDTO(video, author));
        return resp;
    }

    @Override
    public List<VideoReportDTO> listReports(Integer adminRole, Integer videoId) {
        requireAdmin(adminRole);
        if (videoId == null) {
            throw new IllegalArgumentException("videoId 不能为空");
        }
        List<VideoReport> reports = videoReportMapper.selectList(new LambdaQueryWrapper<VideoReport>()
                .eq(VideoReport::getVideoId, videoId)
                .orderByDesc(VideoReport::getCreateTime));
        List<VideoReportDTO> list = new ArrayList<>();
        for (VideoReport report : reports) {
            User reporter = userMapper.selectById(report.getReporterId());
            VideoReportDTO dto = new VideoReportDTO();
            dto.setId(report.getId());
            dto.setVideoId(report.getVideoId());
            dto.setReporterId(report.getReporterId());
            dto.setReason(report.getReason());
            dto.setCreateTime(report.getCreateTime());
            if (reporter != null) {
                dto.setReporterNickname(reporter.getNickname());
            }
            list.add(dto);
        }
        return list;
    }

    private PageResult<VideoDTO> listByStatus(int status, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Video> mpPage = new Page<>(safePage, safeSize);
        videoMapper.selectPage(mpPage, new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, status)
                .orderByDesc(Video::getCreateTime));

        List<VideoDTO> list = new ArrayList<>();
        for (Video video : mpPage.getRecords()) {
            User author = userMapper.selectById(video.getAuthorId());
            list.add(toDTO(video, author));
        }

        PageResult<VideoDTO> result = new PageResult<>();
        result.setRecords(list);
        result.setTotal(mpPage.getTotal());
        result.setPage(safePage);
        result.setSize(safeSize);
        return result;
    }

    @Override
    public CustomResponse approve(Integer adminRole, Integer adminUserId, Integer videoId) {
        if (adminRole == null || adminRole != ROLE_ADMIN) {
            return fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (video.getStatus() != VideoStatus.PENDING && video.getStatus() != VideoStatus.REPORT_REVIEW) {
            return fail(400, "当前状态不可审核通过");
        }
        video.setStatus(VideoStatus.PUBLISHED);
        video.setReportCount(0);
        videoMapper.updateById(video);
        if (adminUserId != null) {
            notificationService.notifyVideoApproved(adminUserId, videoId);
        }
        return ok("已通过审核");
    }

    @Override
    public CustomResponse reject(Integer adminRole, Integer videoId) {
        if (adminRole == null || adminRole != ROLE_ADMIN) {
            return fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        video.setStatus(VideoStatus.PRIVATE);
        videoMapper.updateById(video);
        return ok("已驳回，视频设为仅自己可见");
    }

    @Override
    @Transactional
    public CustomResponse deleteVideo(Integer adminRole, Integer videoId) {
        if (adminRole == null || adminRole != ROLE_ADMIN) {
            return fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        // 先清理引用该视频的子表数据，否则外键约束会让删除失败（play_history/video_reactions/video_reports 外键指向 videos）
        playHistoryMapper.delete(new LambdaQueryWrapper<PlayHistory>().eq(PlayHistory::getVideoId, videoId));
        videoReactionMapper.delete(new LambdaQueryWrapper<VideoReaction>().eq(VideoReaction::getVideoId, videoId));
        videoReportMapper.delete(new LambdaQueryWrapper<VideoReport>().eq(VideoReport::getVideoId, videoId));
        List<Comment> videoComments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, 1)
                .eq(Comment::getTargetId, videoId));
        if (!videoComments.isEmpty()) {
            List<Integer> commentIds = videoComments.stream().map(Comment::getId).toList();
            commentReactionMapper.delete(new LambdaQueryWrapper<CommentReaction>()
                    .in(CommentReaction::getCommentId, commentIds));
            commentMapper.deleteBatchIds(commentIds);
        }
        videoMapper.deleteById(videoId);
        deleteFiles(video);
        return ok("已删除");
    }

    private void deleteFiles(Video video) {
        deleteIfExists(video.getCoverUrl());
        deleteIfExists(video.getVideoUrl());
    }

    private void deleteIfExists(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String name = url.startsWith("/uploads/") ? url.substring("/uploads/".length()) : url;
        Path path = Paths.get(uploadPath, name);
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private VideoDTO toDTO(Video video, User author) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setCoverUrl(video.getCoverUrl());
        dto.setVideoUrl(video.getVideoUrl());
        dto.setAuthorId(video.getAuthorId());
        dto.setStatus(video.getStatus());
        dto.setReportCount(video.getReportCount() != null ? video.getReportCount() : 0);
        dto.setCreateTime(video.getCreateTime());
        if (author != null) {
            dto.setAuthorNickname(author.getNickname());
            dto.setAuthorAvatar(author.getAvatar());
        }
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

    private void requireAdmin(Integer adminRole) {
        if (adminRole == null || adminRole != ROLE_ADMIN) {
            throw new SecurityException("需要管理员权限");
        }
    }
}
