package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.PlayHistoryMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.PlayHistoryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.PlayHistory;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.reaction.ReactionService;
import com.doinb.backend.service.video.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 视频业务：列表、详情、播放历史、本地上传。
 */
@Service
public class VideoServiceImpl implements VideoService {

    /** 0审核中 1已发布 2已下架 */
    private static final int STATUS_PUBLISHED = 1;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    private final PlayHistoryMapper playHistoryMapper;
    private final ReactionService reactionService;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public VideoServiceImpl(VideoMapper videoMapper,
                            UserMapper userMapper,
                            PlayHistoryMapper playHistoryMapper,
                            ReactionService reactionService) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
        this.playHistoryMapper = playHistoryMapper;
        this.reactionService = reactionService;
    }

    @Override
    public PageResult<VideoDTO> listPublished(long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Video> mpPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Video::getCreateTime);
        videoMapper.selectPage(mpPage, wrapper);

        List<VideoDTO> records = toVideoDTOList(mpPage.getRecords());
        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, records);
    }

    @Override
    public CustomResponse getOne(Integer videoId, Integer viewerUserId) {
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null || !Objects.equals(video.getStatus(), STATUS_PUBLISHED)) {
            return fail(404, "视频不存在或未发布");
        }
        VideoDTO dto = toVideoDTO(video);
        dto.setReactions(reactionService.getVideoSummary(videoId, viewerUserId));
        CustomResponse resp = ok("OK");
        resp.setData(dto);
        return resp;
    }

    @Override
    public CustomResponse saveProgress(Integer userId, Integer videoId, Integer progress) {
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        if (progress == null || progress < 0) {
            return fail(400, "播放进度无效");
        }

        Video video = videoMapper.selectById(videoId);
        if (video == null || !Objects.equals(video.getStatus(), STATUS_PUBLISHED)) {
            return fail(404, "视频不存在或未发布");
        }

        PlayHistory existing = playHistoryMapper.selectOne(
                new LambdaQueryWrapper<PlayHistory>()
                        .eq(PlayHistory::getUserId, userId)
                        .eq(PlayHistory::getVideoId, videoId)
        );

        if (existing == null) {
            PlayHistory history = new PlayHistory();
            history.setUserId(userId);
            history.setVideoId(videoId);
            history.setProgress(progress);
            history.setUpdateTime(LocalDateTime.now());
            playHistoryMapper.insert(history);
        } else {
            playHistoryMapper.update(null,
                    new LambdaUpdateWrapper<PlayHistory>()
                            .eq(PlayHistory::getUserId, userId)
                            .eq(PlayHistory::getVideoId, videoId)
                            .set(PlayHistory::getProgress, progress)
                            .set(PlayHistory::getUpdateTime, LocalDateTime.now())
            );
        }
        return ok("进度已保存");
    }

    @Override
    public PageResult<PlayHistoryDTO> listHistory(Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<PlayHistory> mpPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<PlayHistory> wrapper = new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .orderByDesc(PlayHistory::getUpdateTime);
        playHistoryMapper.selectPage(mpPage, wrapper);

        List<PlayHistoryDTO> records = new ArrayList<>();
        for (PlayHistory history : mpPage.getRecords()) {
            Video video = videoMapper.selectById(history.getVideoId());
            if (video == null) {
                continue;
            }
            PlayHistoryDTO dto = new PlayHistoryDTO();
            dto.setVideoId(video.getId());
            dto.setTitle(video.getTitle());
            dto.setCoverUrl(video.getCoverUrl());
            dto.setProgress(history.getProgress());
            dto.setUpdateTime(history.getUpdateTime());
            records.add(dto);
        }
        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, records);
    }

    @Override
    public CustomResponse upload(Integer userId, Integer role, String title, String description,
                                 MultipartFile cover, MultipartFile videoFile) {
        if (!StringUtils.hasText(title)) {
            return fail(400, "标题不能为空");
        }
        if (title.length() > 100) {
            return fail(400, "标题长度不能超过100");
        }
        if (videoFile == null || videoFile.isEmpty()) {
            return fail(400, "请上传视频文件");
        }

        try {
            String videoExt = extensionOf(videoFile.getOriginalFilename(), VIDEO_EXTENSIONS);
            if (videoExt == null) {
                return fail(400, "视频格式仅支持 mp4 / webm / mov");
            }

            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path videoDir = baseDir.resolve("videos");
            Path coverDir = baseDir.resolve("covers");
            Files.createDirectories(videoDir);
            Files.createDirectories(coverDir);

            String videoFileName = UUID.randomUUID() + "." + videoExt;
            Path videoPath = videoDir.resolve(videoFileName);
            videoFile.transferTo(videoPath.toFile());

            String coverUrl = null;
            if (cover != null && !cover.isEmpty()) {
                String coverExt = extensionOf(cover.getOriginalFilename(), IMAGE_EXTENSIONS);
                if (coverExt == null) {
                    return fail(400, "封面格式仅支持 jpg / png / webp / gif");
                }
                String coverFileName = UUID.randomUUID() + "." + coverExt;
                Path coverPath = coverDir.resolve(coverFileName);
                cover.transferTo(coverPath.toFile());
                coverUrl = "/uploads/covers/" + coverFileName;
            }

            Video video = new Video();
            video.setTitle(title.trim());
            video.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            video.setAuthorId(userId);
            video.setCoverUrl(coverUrl);
            video.setVideoUrl("/uploads/videos/" + videoFileName);
            // 课程演示：上传后直接发布；正式环境可改为 0（审核中）
            video.setStatus(STATUS_PUBLISHED);
            video.setCreateTime(LocalDateTime.now());
            videoMapper.insert(video);

            CustomResponse resp = ok("上传成功");
            resp.setData(toVideoDTO(video));
            return resp;
        } catch (IOException e) {
            return fail(500, "文件保存失败：" + e.getMessage());
        }
    }

    @Override
    public PageResult<VideoDTO> listMyVideos(Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Video> mpPage = new Page<>(safePage, safeSize);
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<Video>()
                .eq(Video::getAuthorId, userId)
                .orderByDesc(Video::getCreateTime);
        videoMapper.selectPage(mpPage, wrapper);

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toVideoDTOList(mpPage.getRecords()));
    }

    @Override
    public CustomResponse updateVideo(Integer userId, Integer role, Integer videoId,
                                      String title, String description,
                                      MultipartFile cover, MultipartFile videoFile) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (!canManageVideo(userId, role, video)) {
            return fail(403, "无权编辑该视频");
        }
        if (!StringUtils.hasText(title)) {
            return fail(400, "标题不能为空");
        }
        if (title.length() > 100) {
            return fail(400, "标题长度不能超过100");
        }

        try {
            LambdaUpdateWrapper<Video> wrapper = new LambdaUpdateWrapper<Video>()
                    .eq(Video::getId, videoId)
                    .set(Video::getTitle, title.trim())
                    .set(Video::getDescription, StringUtils.hasText(description) ? description.trim() : null);

            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path coverDir = baseDir.resolve("covers");
            Path videoDir = baseDir.resolve("videos");

            if (cover != null && !cover.isEmpty()) {
                String coverExt = extensionOf(cover.getOriginalFilename(), IMAGE_EXTENSIONS);
                if (coverExt == null) {
                    return fail(400, "封面格式仅支持 jpg / png / webp / gif");
                }
                Files.createDirectories(coverDir);
                String coverFileName = UUID.randomUUID() + "." + coverExt;
                cover.transferTo(coverDir.resolve(coverFileName).toFile());
                wrapper.set(Video::getCoverUrl, "/uploads/covers/" + coverFileName);
            }

            if (videoFile != null && !videoFile.isEmpty()) {
                String videoExt = extensionOf(videoFile.getOriginalFilename(), VIDEO_EXTENSIONS);
                if (videoExt == null) {
                    return fail(400, "视频格式仅支持 mp4 / webm / mov");
                }
                Files.createDirectories(videoDir);
                String videoFileName = UUID.randomUUID() + "." + videoExt;
                videoFile.transferTo(videoDir.resolve(videoFileName).toFile());
                wrapper.set(Video::getVideoUrl, "/uploads/videos/" + videoFileName);
            }

            videoMapper.update(null, wrapper);
            Video updated = videoMapper.selectById(videoId);
            CustomResponse resp = ok("更新成功");
            resp.setData(toVideoDTO(updated));
            return resp;
        } catch (IOException e) {
            return fail(500, "文件保存失败：" + e.getMessage());
        }
    }

    @Override
    public CustomResponse getMyVideo(Integer userId, Integer role, Integer videoId) {
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (!canManageVideo(userId, role, video)) {
            return fail(403, "无权查看该视频");
        }
        CustomResponse resp = ok("OK");
        resp.setData(toVideoDTO(video));
        return resp;
    }

    @Override
    public CustomResponse updateStatus(Integer userId, Integer role, Integer videoId, Integer status) {
        if (status == null || status < 0 || status > 2) {
            return fail(400, "状态值无效（0审核中 1已发布 2已下架）");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (!canManageVideo(userId, role, video)) {
            return fail(403, "无权修改该视频状态");
        }

        videoMapper.update(null, new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, videoId)
                .set(Video::getStatus, status));
        return ok("状态已更新");
    }

    @Override
    public CustomResponse deleteVideo(Integer userId, Integer role, Integer videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (!canManageVideo(userId, role, video)) {
            return fail(403, "无权删除该视频");
        }
        videoMapper.deleteById(videoId);
        return ok("删除成功");
    }

    /** 作者本人或管理员可管理视频 */
    private boolean canManageVideo(Integer userId, Integer role, Video video) {
        if (role != null && role == 2) {
            return true;
        }
        return Objects.equals(video.getAuthorId(), userId);
    }

    private List<VideoDTO> toVideoDTOList(List<Video> videos) {
        if (videos.isEmpty()) {
            return List.of();
        }
        List<Integer> authorIds = videos.stream()
                .map(Video::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> authorMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<VideoDTO> list = new ArrayList<>();
        for (Video video : videos) {
            list.add(toVideoDTO(video, authorMap.get(video.getAuthorId())));
        }
        return list;
    }

    private VideoDTO toVideoDTO(Video video) {
        User author = userMapper.selectById(video.getAuthorId());
        return toVideoDTO(video, author);
    }

    private VideoDTO toVideoDTO(Video video, User author) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setAuthorId(video.getAuthorId());
        dto.setAuthorNickname(author != null ? author.getNickname() : "未知作者");
        dto.setAuthorAvatar(author != null ? author.getAvatar() : null);
        dto.setCoverUrl(video.getCoverUrl());
        dto.setVideoUrl(video.getVideoUrl());
        dto.setStatus(video.getStatus());
        dto.setCreateTime(video.getCreateTime());
        return dto;
    }

    private String extensionOf(String filename, Set<String> allowed) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return allowed.contains(ext) ? ext : null;
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
