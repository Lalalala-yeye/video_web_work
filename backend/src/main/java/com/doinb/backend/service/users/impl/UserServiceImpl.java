package com.doinb.backend.service.users.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户资料相关业务：查用户信息、改昵称/头像。
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDTO getUserById(Integer id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return null;
        }
        return toDTO(user);
    }

    @Override
    public CustomResponse updateUserInfo(Integer userId, String nickname, String avatar) {
        if (!StringUtils.hasText(nickname)) {
            return fail(500, "昵称不能为空");
        }
        if (nickname.length() > 50) {
            return fail(500, "昵称长度不能超过50");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getNickname, nickname.trim());
        if (StringUtils.hasText(avatar)) {
            wrapper.set(User::getAvatar, avatar.trim());
        }
        userMapper.update(null, wrapper);

        return ok("资料更新成功");
    }

    /** 把数据库实体转成返回给前端的 DTO（去掉密码） */
    public static UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
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
