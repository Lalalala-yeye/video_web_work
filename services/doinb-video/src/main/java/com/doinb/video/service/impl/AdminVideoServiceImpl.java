package com.doinb.video.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.dto.VideoDTO;
import com.doinb.video.client.MessageNotifier;
import com.doinb.video.client.UserDirectory;
import com.doinb.video.mapper.VideoMapper;
import com.doinb.video.mapper.VideoReportMapper;
import com.doinb.video.pojo.VideoStatus;
import com.doinb.video.pojo.dto.VideoReportDTO;
import com.doinb.video.pojo.entity.Video;
import com.doinb.video.pojo.entity.VideoReport;
import com.doinb.video.service.AdminVideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminVideoServiceImpl implements AdminVideoService {

    private final VideoMapper videoMapper;
    private final VideoReportMapper videoReportMapper;
    private final UserDirectory userDirectory;
    private final MessageNotifier messageNotifier;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public AdminVideoServiceImpl(VideoMapper videoMapper,
                                 VideoReportMapper videoReportMapper,
                                 UserDirectory userDirectory,
                                 MessageNotifier messageNotifier) {
        this.videoMapper = videoMapper;
        this.videoReportMapper = videoReportMapper;
        this.userDirectory = userDirectory;
        this.messageNotifier = messageNotifier;
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
        if (adminRole == null || adminRole != VideoStatus.ROLE_ADMIN) {
            return CustomResponse.fail(403, "需要管理员权限");
        }
        if (videoId == null) {
            return CustomResponse.fail(400, "视频 id 不能为空");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return CustomResponse.fail(404, "视频不存在");
        }
        return CustomResponse.ok("OK", toDTO(video, userDirectory.findById(video.getAuthorId())));
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
            UserDTO reporter = userDirectory.findById(report.getReporterId());
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

    @Override
    public CustomResponse approve(Integer adminRole, Integer adminUserId, Integer videoId) {
        if (adminRole == null || adminRole != VideoStatus.ROLE_ADMIN) {
            return CustomResponse.fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return CustomResponse.fail(404, "视频不存在");
        }
        if (video.getStatus() != VideoStatus.PENDING && video.getStatus() != VideoStatus.REPORT_REVIEW) {
            return CustomResponse.fail(400, "当前状态不可审核通过");
        }
        video.setStatus(VideoStatus.PUBLISHED);
        video.setReportCount(0);
        videoMapper.updateById(video);
        if (adminUserId != null) {
            messageNotifier.notifyVideoApproved(adminUserId, video.getAuthorId(), videoId, video.getTitle());
        }
        return CustomResponse.ok("已通过审核", null);
    }

    @Override
    public CustomResponse reject(Integer adminRole, Integer videoId) {
        if (adminRole == null || adminRole != VideoStatus.ROLE_ADMIN) {
            return CustomResponse.fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return CustomResponse.fail(404, "视频不存在");
        }
        video.setStatus(VideoStatus.PRIVATE);
        videoMapper.updateById(video);
        return CustomResponse.ok("已驳回，视频设为仅自己可见", null);
    }

    @Override
    public CustomResponse deleteVideo(Integer adminRole, Integer videoId) {
        if (adminRole == null || adminRole != VideoStatus.ROLE_ADMIN) {
            return CustomResponse.fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return CustomResponse.fail(404, "视频不存在");
        }
        deleteFiles(video);
        videoMapper.deleteById(videoId);
        return CustomResponse.ok("已删除", null);
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
            list.add(toDTO(video, userDirectory.findById(video.getAuthorId())));
        }
        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, list);
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
        try {
            Files.deleteIfExists(Paths.get(uploadPath, name));
        } catch (Exception ignored) {
        }
    }

    private VideoDTO toDTO(Video video, UserDTO author) {
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
        dto.setAuthorNickname(author != null ? author.getNickname() : "未知作者");
        dto.setAuthorAvatar(author != null ? author.getAvatar() : null);
        return dto;
    }

    private void requireAdmin(Integer adminRole) {
        if (adminRole == null || adminRole != VideoStatus.ROLE_ADMIN) {
            throw new SecurityException("需要管理员权限");
        }
    }
}
