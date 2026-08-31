package com.doinb.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.user.mapper.UserMapper;
import com.doinb.user.pojo.dto.UserPublicDTO;
import com.doinb.user.pojo.dto.UserShowcaseDTO;
import com.doinb.user.pojo.entity.User;
import com.doinb.user.service.UserService;
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

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final UserMapper userMapper;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
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
        UserShowcaseDTO showcase = new UserShowcaseDTO();
        showcase.setProfile(profile);
        showcase.setVideos(List.of());
        showcase.setVideoTotal(0);
        showcase.setFollowing(false);
        return showcase;
    }

    @Override
    public List<UserDTO> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectBatchIds(ids);
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            result.add(toDTO(user));
        }
        return result;
    }

    @Override
    public List<UserDTO> search(String keyword, long limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        long safeLimit = limit < 1 ? 10 : Math.min(limit, 50);
        String kw = keyword.trim();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .orderByDesc(User::getId)
                .last("LIMIT " + safeLimit));
        List<UserDTO> result = new ArrayList<>();
        for (User user : users) {
            result.add(toDTO(user));
        }
        return result;
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
            return CustomResponse.fail(500, "昵称不能为空");
        }
        if (nickname.length() > 50) {
            return CustomResponse.fail(500, "昵称长度不能超过50");
        }
        if (bio != null && bio.length() > 500) {
            return CustomResponse.fail(500, "个人简介不能超过500字");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, nickname.trim())
                .set(User::getBio, StringUtils.hasText(bio) ? bio.trim() : null));
        return CustomResponse.ok("资料更新成功", null);
    }

    @Override
    public CustomResponse uploadAvatar(Integer userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return CustomResponse.fail(400, "请选择头像图片");
        }
        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null) {
            return CustomResponse.fail(400, "头像格式仅支持 jpg / png / webp / gif");
        }
        try {
            Path baseDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Path avatarDir = baseDir.resolve("avatars");
            Files.createDirectories(avatarDir);
            String fileName = UUID.randomUUID() + "." + ext;
            file.transferTo(avatarDir.resolve(fileName).toFile());

            String avatarUrl = "/uploads/avatars/" + fileName;
            userMapper.update(null, new LambdaUpdateWrapper<User>()
                    .eq(User::getId, userId)
                    .set(User::getAvatar, avatarUrl));
            return CustomResponse.ok("头像更新成功", getUserById(userId));
        } catch (IOException e) {
            return CustomResponse.fail(500, "头像保存失败：" + e.getMessage());
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

    private String extensionOf(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return null;
        }
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext) ? ext : null;
    }
}
