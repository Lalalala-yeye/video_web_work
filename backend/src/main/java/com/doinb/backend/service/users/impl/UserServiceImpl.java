package com.doinb.backend.service.users.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.dto.UserPublicDTO;
import com.doinb.backend.pojo.dto.UserShowcaseDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.users.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final int STATUS_PUBLISHED = 1;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final UserMapper userMapper;
    private final VideoMapper videoMapper;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public UserServiceImpl(UserMapper userMapper, VideoMapper videoMapper) {
        this.userMapper = userMapper;
        this.videoMapper = videoMapper;
    }

    @Override
    public UserDTO getUserById(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }
        ensurePublisherRole(user);
        return toDTO(user);
    }

    @Override
    public UserPublicDTO getPublicProfile(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }
        return toPublicDTO(user);
    }

    @Override
    public UserShowcaseDTO getShowcase(Integer id, long page, long size) {
        UserPublicDTO profile = getPublicProfile(id);
        if (profile == null) {
            return null;
        }

        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 12 : Math.min(size, 50);

        Page<Video> mpPage = new Page<>(safePage, safeSize);
        videoMapper.selectPage(mpPage, new LambdaQueryWrapper<Video>()
                .eq(Video::getAuthorId, id)
                .eq(Video::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Video::getCreateTime));

        User author = userMapper.selectById(id);
        List<VideoDTO> videos = new ArrayList<>();
        for (Video video : mpPage.getRecords()) {
            videos.add(toVideoDTO(video, author));
        }

        UserShowcaseDTO showcase = new UserShowcaseDTO();
        showcase.setProfile(profile);
        showcase.setVideos(videos);
        showcase.setVideoTotal(mpPage.getTotal());
        return showcase;
    }

    @Override
    public void ensurePublisherRole(User user) {
        if (user == null) {
            return;
        }
        Integer role = user.getRole();
        if (role != null && role >= 1) {
            return;
        }
        user.setRole(1);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getRole, 1));
    }

    @Override
    public CustomResponse updateUserInfo(Integer userId, String nickname, String bio) {
        if (!StringUtils.hasText(nickname)) {
            return fail(500, "昵称不能为空");
        }
        if (nickname.length() > 50) {
            return fail(500, "昵称长度不能超过50");
        }
        if (bio != null && bio.length() > 500) {
            return fail(500, "个人简介不能超过500字");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, nickname.trim())
                .set(User::getBio, StringUtils.hasText(bio) ? bio.trim() : null);
        userMapper.update(null, wrapper);

        return ok("资料更新成功");
    }

    @Override
    public CustomResponse uploadAvatar(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return fail(400, "请选择头像图片");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null) {
            return fail(400, "头像格式仅支持 jpg / png / webp / gif");
        }

        try {
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path avatarDir = baseDir.resolve("avatars");
            Files.createDirectories(avatarDir);

            String fileName = UUID.randomUUID() + "." + ext;
            Path target = avatarDir.resolve(fileName);
            file.transferTo(target.toFile());

            String avatarUrl = "/uploads/avatars/" + fileName;
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getAvatar, avatarUrl));

            UserDTO dto = getUserById(userId);
            CustomResponse resp = ok("头像更新成功");
            resp.setData(dto);
            return resp;
        } catch (IOException e) {
            return fail(500, "头像保存失败：" + e.getMessage());
        }
    }

    public static UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        dto.setBio(user.getBio());
        return dto;
    }

    public static UserPublicDTO toPublicDTO(User user) {
        UserPublicDTO dto = new UserPublicDTO();
        dto.setId(user.getId());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setBio(user.getBio());
        return dto;
    }

    private VideoDTO toVideoDTO(Video video, User author) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setAuthorId(video.getAuthorId());
        dto.setAuthorNickname(author != null ? author.getNickname() : "未知作者");
        dto.setCoverUrl(video.getCoverUrl());
        dto.setVideoUrl(video.getVideoUrl());
        dto.setStatus(video.getStatus());
        dto.setCreateTime(video.getCreateTime());
        return dto;
    }

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext) ? ext : null;
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
