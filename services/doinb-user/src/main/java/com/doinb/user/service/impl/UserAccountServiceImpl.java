package com.doinb.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.jwt.JwtUtil;
import com.doinb.user.mapper.UserMapper;
import com.doinb.user.pojo.entity.User;
import com.doinb.user.service.UserAccountService;
import com.doinb.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserAccountServiceImpl implements UserAccountService {

    private static final String DEFAULT_AVATAR =
            "https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png";

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserAccountServiceImpl(UserMapper userMapper,
                                  UserService userService,
                                  PasswordEncoder passwordEncoder,
                                  JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public CustomResponse register(String username, String password, String confirmedPassword) {
        if (!StringUtils.hasText(username)) {
            return CustomResponse.fail(403, "账号不能为空");
        }
        if (!StringUtils.hasText(password) || !StringUtils.hasText(confirmedPassword)) {
            return CustomResponse.fail(403, "密码不能为空");
        }

        username = username.trim();
        if (username.length() > 50) {
            return CustomResponse.fail(403, "账号长度不能超过50");
        }
        if (password.length() > 50) {
            return CustomResponse.fail(403, "密码长度不能超过50");
        }
        if (!password.equals(confirmedPassword)) {
            return CustomResponse.fail(403, "两次输入的密码不一致");
        }
        if (userMapper.selectByUsername(username) != null) {
            return CustomResponse.fail(403, "账号已存在");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNickname("用户_" + username);
        newUser.setAvatar(DEFAULT_AVATAR);
        newUser.setRole(1);
        userMapper.insert(newUser);
        return CustomResponse.ok("注册成功，请登录", null);
    }

    @Override
    public CustomResponse login(String username, String password) {
        return doLogin(username, password, "user", false);
    }

    @Override
    public CustomResponse adminLogin(String username, String password) {
        return doLogin(username, password, "admin", true);
    }

    private CustomResponse doLogin(String username, String password, String tokenRole, boolean requireAdmin) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return CustomResponse.fail(403, "账号或密码不能为空");
        }

        User user = userMapper.selectByUsername(username.trim());
        if (user == null) {
            return CustomResponse.fail(403, "账号不存在");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            return CustomResponse.fail(403, "账号或密码不正确");
        }
        if (requireAdmin && (user.getRole() == null || user.getRole() != 2)) {
            return CustomResponse.fail(403, "您不是管理员，无权访问");
        }

        userService.ensurePublisherRole(user);
        String token = jwtUtil.createToken(user.getId(), tokenRole);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", UserServiceImpl.toDTO(user));
        return CustomResponse.ok("登录成功", data);
    }

    @Override
    public CustomResponse personalInfo(Integer userId) {
        UserDTO userDTO = userService.getUserById(userId);
        if (userDTO == null) {
            return CustomResponse.fail(404, "用户不存在");
        }
        return CustomResponse.ok("OK", userDTO);
    }

    @Override
    public CustomResponse adminPersonalInfo(Integer userId, boolean admin) {
        if (!admin) {
            return CustomResponse.fail(403, "您不是管理员，无权访问");
        }
        return personalInfo(userId);
    }

    @Override
    public CustomResponse logout() {
        return CustomResponse.ok("已退出登录", null);
    }

    @Override
    public CustomResponse updatePassword(Integer userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            return CustomResponse.fail(500, "新密码不能为空");
        }
        if (newPassword.length() > 50) {
            return CustomResponse.fail(500, "新密码长度不能超过50");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            return CustomResponse.fail(404, "用户不存在");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return CustomResponse.fail(403, "旧密码不正确");
        }
        if (oldPassword.equals(newPassword)) {
            return CustomResponse.fail(500, "新密码不能与旧密码相同");
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(newPassword)));
        logout();
        return CustomResponse.ok("密码修改成功，请重新登录", null);
    }
}
