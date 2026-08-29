package com.doinb.backend.service.video.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import com.doinb.backend.pojo.dto.PlayHistoryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.PlayHistory;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.pojo.entity.VideoReport;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.CommentReaction;
import com.doinb.backend.pojo.entity.VideoReaction;
import com.doinb.backend.service.reaction.ReactionService;
import com.doinb.backend.service.video.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
public class VideoServiceImpl implements VideoService {

    private static final int ROLE_ADMIN = 2;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final VideoMapper videoMapper;
    private final UserMapper userMapper;
    private final PlayHistoryMapper playHistoryMapper;
    private final VideoReportMapper videoReportMapper;
    private final VideoReactionMapper videoReactionMapper;
    private final CommentMapper commentMapper;
    private final CommentReactionMapper commentReactionMapper;
    private final ReactionService reactionService;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public VideoServiceImpl(VideoMapper videoMapper,
                            UserMapper userMapper,
                            PlayHistoryMapper playHistoryMapper,
                            VideoReportMapper videoReportMapper,
                            VideoReactionMapper videoReactionMapper,
                            CommentMapper commentMapper,
                            CommentReactionMapper commentReactionMapper,
                            ReactionService reactionService) {
        this.videoMapper = videoMapper;
        this.userMapper = userMapper;
        this.playHistoryMapper = playHistoryMapper;
        this.videoReportMapper = videoReportMapper;
        this.videoReactionMapper = videoReactionMapper;
        this.commentMapper = commentMapper;
        this.commentReactionMapper = commentReactionMapper;
        this.reactionService = reactionService;
    }

    @Override
    public PageResult<VideoDTO> listPublished(long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Video> mpPage = new Page<>(safePage, safeSize);
        videoMapper.selectPage(mpPage, new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, VideoStatus.PUBLISHED)
                .orderByDesc(Video::getCreateTime));

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toVideoDTOList(mpPage.getRecords()));
    }

    @Override
    public CustomResponse getOne(Integer videoId, Integer viewerUserId, Integer viewerRole) {
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null || !canViewVideo(video, viewerUserId, viewerRole)) {
            return fail(404, "视频不存在或未发布");
        }
        VideoDTO dto = toVideoDTO(video);
        if (Objects.equals(video.getStatus(), VideoStatus.PUBLISHED)) {
            dto.setReactions(reactionService.getVideoSummary(videoId, viewerUserId));
        }
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
        if (video == null || !Objects.equals(video.getStatus(), VideoStatus.PUBLISHED)) {
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
        playHistoryMapper.selectPage(mpPage, new LambdaQueryWrapper<PlayHistory>()
                .eq(PlayHistory::getUserId, userId)
                .orderByDesc(PlayHistory::getUpdateTime));

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
                                 String visibility, MultipartFile cover, MultipartFile videoFile) {
        if (!StringUtils.hasText(title)) {
            return fail(400, "标题不能为空");
        }
        if (title.length() > 100) {
            return fail(400, "标题长度不能超过100");
        }
        if (videoFile == null || videoFile.isEmpty()) {
            return fail(400, "请上传视频文件");
        }

        int status = resolveVisibilityStatus(visibility, true);

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
            videoFile.transferTo(videoDir.resolve(videoFileName).toFile());

            String coverUrl = null;
            if (cover != null && !cover.isEmpty()) {
                String coverExt = extensionOf(cover.getOriginalFilename(), IMAGE_EXTENSIONS);
                if (coverExt == null) {
                    return fail(400, "封面格式仅支持 jpg / png / webp / gif");
                }
                String coverFileName = UUID.randomUUID() + "." + coverExt;
                cover.transferTo(coverDir.resolve(coverFileName).toFile());
                coverUrl = "/uploads/covers/" + coverFileName;
            }

            Video video = new Video();
            video.setTitle(title.trim());
            video.setDescription(StringUtils.hasText(description) ? description.trim() : null);
            video.setAuthorId(userId);
            video.setCoverUrl(coverUrl);
            video.setVideoUrl("/uploads/videos/" + videoFileName);
            video.setStatus(status);
            video.setReportCount(0);
            video.setCreateTime(LocalDateTime.now());
            videoMapper.insert(video);

            CustomResponse resp = ok(status == VideoStatus.PRIVATE ? "上传成功（仅自己可见）" : "上传成功，等待管理员审核");
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
        videoMapper.selectPage(mpPage, new LambdaQueryWrapper<Video>()
                .eq(Video::getAuthorId, userId)
                .orderByDesc(Video::getCreateTime));

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toVideoDTOList(mpPage.getRecords()));
    }

    @Override
    public CustomResponse updateVideo(Integer userId, Integer role, Integer videoId,
                                      String title, String description, String visibility,
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

            int newStatus = resolveVisibilityStatus(visibility, false);
            if (isPrivateVisibility(visibility)) {
                wrapper.set(Video::getStatus, VideoStatus.PRIVATE);
            } else if (isPublicVisibility(visibility)) {
                wrapper.set(Video::getStatus, VideoStatus.PENDING);
            }

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
                if (isPublicVisibility(visibility)) {
                    wrapper.set(Video::getStatus, VideoStatus.PENDING);
                }
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
                if (isPublicVisibility(visibility)) {
                    wrapper.set(Video::getStatus, VideoStatus.PENDING);
                }
            }

            videoMapper.update(null, wrapper);
            Video updated = videoMapper.selectById(videoId);
            String msg = newStatus == VideoStatus.PRIVATE ? "更新成功（仅自己可见）" : "更新成功，等待管理员审核";
            CustomResponse resp = ok(msg);
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
    public CustomResponse setVisibility(Integer userId, Integer role, Integer videoId, String visibility) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (!canManageVideo(userId, role, video)) {
            return fail(403, "无权修改该视频");
        }
        if (!isPublicVisibility(visibility) && !isPrivateVisibility(visibility)) {
            return fail(400, "可见性参数无效，请使用 public 或 private");
        }

        int status = isPrivateVisibility(visibility) ? VideoStatus.PRIVATE : VideoStatus.PENDING;
        videoMapper.update(null, new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, videoId)
                .set(Video::getStatus, status));

        return ok(status == VideoStatus.PRIVATE ? "已设为仅自己可见" : "已提交审核，等待管理员处理");
    }

    @Override
    public CustomResponse reportVideo(Integer userId, Integer videoId, String reason) {
        if (videoId == null) {
            return fail(400, "视频 id 不能为空");
        }
        Video video = videoMapper.selectById(videoId);
        if (video == null || !Objects.equals(video.getStatus(), VideoStatus.PUBLISHED)) {
            return fail(404, "只能举报已发布的视频");
        }
        if (Objects.equals(video.getAuthorId(), userId)) {
            return fail(400, "不能举报自己的视频");
        }

        Long existing = videoReportMapper.selectCount(new LambdaQueryWrapper<VideoReport>()
                .eq(VideoReport::getVideoId, videoId)
                .eq(VideoReport::getReporterId, userId));
        if (existing != null && existing > 0) {
            return fail(400, "您已举报过该视频");
        }

        VideoReport report = new VideoReport();
        report.setVideoId(videoId);
        report.setReporterId(userId);
        report.setReason(StringUtils.hasText(reason) ? reason.trim() : null);
        report.setCreateTime(LocalDateTime.now());
        videoReportMapper.insert(report);

        Long reportCount = videoReportMapper.selectCount(new LambdaQueryWrapper<VideoReport>()
                .eq(VideoReport::getVideoId, videoId));
        int count = reportCount != null ? reportCount.intValue() : 1;

        LambdaUpdateWrapper<Video> update = new LambdaUpdateWrapper<Video>()
                .eq(Video::getId, videoId)
                .set(Video::getReportCount, count);
        if (count >= VideoStatus.REPORT_THRESHOLD) {
            update.set(Video::getStatus, VideoStatus.REPORT_REVIEW);
        }
        videoMapper.update(null, update);

        return ok(count >= VideoStatus.REPORT_THRESHOLD ? "举报已提交，该视频已进入复审" : "举报已提交");
    }

    @Override
    @Transactional
    public CustomResponse deleteVideo(Integer userId, Integer role, Integer videoId) {
        Video video = videoMapper.selectById(videoId);
        if (video == null) {
            return fail(404, "视频不存在");
        }
        if (!canManageVideo(userId, role, video)) {
            return fail(403, "无权删除该视频");
        }
        deleteVideoRelations(videoId);
        videoMapper.deleteById(videoId);
        deleteFiles(video);
        return ok("删除成功");
    }

    /**
     * 删除 videos 行之前必须先清理引用它的子表数据（play_history / video_reactions /
     * video_reports 外键指向 videos；comment_reactions 外键指向 comments），
     * 否则删除会因外键约束失败。
     */
    private void deleteVideoRelations(Integer videoId) {
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
    }

    private boolean canViewVideo(Video video, Integer viewerUserId, Integer viewerRole) {
        if (Objects.equals(video.getStatus(), VideoStatus.PUBLISHED)) {
            return true;
        }
        if (viewerRole != null && viewerRole == ROLE_ADMIN) {
            return true;
        }
        return viewerUserId != null && Objects.equals(video.getAuthorId(), viewerUserId);
    }

    private boolean canManageVideo(Integer userId, Integer role, Video video) {
        if (role != null && role == ROLE_ADMIN) {
            return true;
        }
        return Objects.equals(video.getAuthorId(), userId);
    }

    private int resolveVisibilityStatus(String visibility, boolean defaultPublic) {
        if (isPrivateVisibility(visibility)) {
            return VideoStatus.PRIVATE;
        }
        if (isPublicVisibility(visibility) || defaultPublic) {
            return VideoStatus.PENDING;
        }
        return VideoStatus.PENDING;
    }

    private boolean isPublicVisibility(String visibility) {
        return "public".equalsIgnoreCase(visibility);
    }

    private boolean isPrivateVisibility(String visibility) {
        return "private".equalsIgnoreCase(visibility);
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
        dto.setReportCount(video.getReportCount() != null ? video.getReportCount() : 0);
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
