package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.VideoStatus;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.video.AdminVideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public AdminVideoServiceImpl(VideoMapper videoMapper, UserMapper userMapper) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<VideoDTO> listPending(long page, long size) {
        return listByStatus(VideoStatus.PENDING, page, size);
    }

    @Override
    public PageResult<VideoDTO> listReportReview(long page, long size) {
        return listByStatus(VideoStatus.REPORT_REVIEW, page, size);
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
    public CustomResponse approve(Integer adminRole, Integer videoId) {
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
    public CustomResponse deleteVideo(Integer adminRole, Integer videoId) {
        if (adminRole == null || adminRole != ROLE_ADMIN) {
            return fail(403, "需要管理员权限");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        deleteFiles(video);
        videoMapper.deleteById(videoId);
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
}
