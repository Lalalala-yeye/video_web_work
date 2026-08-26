package com.doinb.backend.service.users.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserAccountService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.utils.CurrentUser;
import com.doinb.backend.utils.JwtUtil;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 账号业务实现：注册、登录、改密码等。
 */
@Service
public class UserAccountServiceImpl implements UserAccountService {

    /** 默认头像（注册时若未上传则使用） */
    private static final String DEFAULT_AVATAR =
            "https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png";

    private final UserMapper userMapper;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationProvider authenticationProvider;
    private final JwtUtil jwtUtil;
    private final CurrentUser currentUser;

    public UserAccountServiceImpl(UserMapper userMapper,
                                  UserService userService,
                                  PasswordEncoder passwordEncoder,
                                  AuthenticationProvider authenticationProvider,
                                  JwtUtil jwtUtil,
                                  CurrentUser currentUser) {
        this.userMapper = userMapper;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationProvider = authenticationProvider;
        this.jwtUtil = jwtUtil;
        this.currentUser = currentUser;
    }

    @Override
    public CustomResponse register(String username, String password, String confirmedPassword) {
        if (!StringUtils.hasText(username)) {
            return fail(403, "账号不能为空");
        }
        if (!StringUtils.hasText(password) || !StringUtils.hasText(confirmedPassword)) {
            return fail(403, "密码不能为空");
        }

        username = username.trim();
        if (username.length() > 50) {
            return fail(403, "账号长度不能超过50");
        }
        if (password.length() > 50) {
            return fail(403, "密码长度不能超过50");
        }
        if (!password.equals(confirmedPassword)) {
            return fail(403, "两次输入的密码不一致");
        }

        if (userMapper.selectByUsername(username) != null) {
            return fail(403, "账号已存在");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setNickname("用户_" + username);
        newUser.setAvatar(DEFAULT_AVATAR);
        newUser.setRole(1);
        userMapper.insert(newUser);

        return ok("注册成功，请登录");
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
            return fail(403, "账号或密码不能为空");
        }

        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username.trim(), password);
        Authentication authenticate;
        try {
            authenticate = authenticationProvider.authenticate(authRequest);
        } catch (UsernameNotFoundException e) {
            return fail(403, "账号不存在");
        } catch (BadCredentialsException e) {
            return fail(403, "账号或密码不正确");
        } catch (AuthenticationException e) {
            return fail(403, "账号或密码不正确");
        }
        UserDetailsImpl loginUser = (UserDetailsImpl) authenticate.getPrincipal();
        User user = loginUser.getUser();

        if (requireAdmin && (user.getRole() == null || user.getRole() != 2)) {
            return fail(403, "您不是管理员，无权访问");
        }

        userService.ensurePublisherRole(user);

        String token = jwtUtil.createToken(user.getId(), tokenRole);

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", UserServiceImpl.toDTO(user));

        CustomResponse resp = ok("登录成功");
        resp.setData(data);
        return resp;
    }

    @Override
    public CustomResponse personalInfo() {
        Integer userId = currentUser.getUserId();
        UserDTO userDTO = userService.getUserById(userId);
        if (userDTO == null) {
            return fail(404, "用户不存在");
        }
        CustomResponse resp = ok("OK");
        resp.setData(userDTO);
        return resp;
    }

    @Override
    public CustomResponse adminPersonalInfo() {
        if (!currentUser.isAdmin()) {
            return fail(403, "您不是管理员，无权访问");
        }
        return personalInfo();
    }

    @Override
    public CustomResponse logout() {
        SecurityContextHolder.clearContext();
        return ok("已退出登录");
    }

    @Override
    public CustomResponse updatePassword(String oldPassword, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            return fail(500, "新密码不能为空");
        }
        if (newPassword.length() > 50) {
            return fail(500, "新密码长度不能超过50");
        }

        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        User user = loginUser.getUser();

        UsernamePasswordAuthenticationToken checkToken =
                new UsernamePasswordAuthenticationToken(user.getUsername(), oldPassword);
        try {
            authenticationProvider.authenticate(checkToken);
        } catch (BadCredentialsException e) {
            return fail(403, "旧密码不正确");
        }

        if (oldPassword.equals(newPassword)) {
            return fail(500, "新密码不能与旧密码相同");
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(newPassword));
        userMapper.update(null, wrapper);

        logout();
        return ok("密码修改成功，请重新登录");
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
