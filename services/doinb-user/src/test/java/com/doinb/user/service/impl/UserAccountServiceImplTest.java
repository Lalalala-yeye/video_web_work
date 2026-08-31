package com.doinb.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.jwt.JwtUtil;
import com.doinb.user.mapper.UserMapper;
import com.doinb.user.pojo.entity.User;
import com.doinb.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAccountServiceImplTest {

    private UserMapper userMapper;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private JwtUtil jwtUtil;
    private UserAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtUtil = mock(JwtUtil.class);
        service = new UserAccountServiceImpl(userMapper, userService, passwordEncoder, jwtUtil);
    }

    @Test
    void register_whenPasswordsDiffer_returns403() {
        CustomResponse resp = service.register("user_a", "123456", "654321");
        assertEquals(403, resp.getCode());
        assertEquals("两次输入的密码不一致", resp.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_whenUsernameExists_returns403() {
        when(userMapper.selectByUsername("user_a")).thenReturn(new User());
        CustomResponse resp = service.register("user_a", "123456", "123456");
        assertEquals(403, resp.getCode());
        assertEquals("账号已存在", resp.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_whenUsernameBlank_returns403() {
        CustomResponse resp = service.register("  ", "123456", "123456");
        assertEquals(403, resp.getCode());
        assertEquals("账号不能为空", resp.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_whenValid_insertsUserAndReturns200() {
        when(userMapper.selectByUsername("user_a")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("hashed");
        CustomResponse resp = service.register("user_a", "123456", "123456");
        assertEquals(200, resp.getCode());
        assertEquals("注册成功，请登录", resp.getMessage());
        assertNull(resp.getData());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void login_whenPasswordBlank_returns403() {
        CustomResponse resp = service.login("user_a", "  ");
        assertEquals(403, resp.getCode());
        assertEquals("账号或密码不能为空", resp.getMessage());
    }

    @Test
    void login_whenCredentialsWrong_returns403() {
        User user = new User();
        user.setPassword("hashed");
        when(userMapper.selectByUsername("user_a")).thenReturn(user);
        when(passwordEncoder.matches("wrong_pass", "hashed")).thenReturn(false);
        CustomResponse resp = service.login("user_a", "wrong_pass");
        assertEquals(403, resp.getCode());
        assertEquals("账号或密码不正确", resp.getMessage());
    }

    @Test
    void login_whenUserMissing_returns403() {
        when(userMapper.selectByUsername("no_such_user")).thenReturn(null);
        CustomResponse resp = service.login("no_such_user", "123456");
        assertEquals(403, resp.getCode());
        assertEquals("账号不存在", resp.getMessage());
    }

    @Test
    void login_whenValid_returnsToken() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setNickname("用户_user_a");
        user.setRole(1);
        user.setPassword("hashed");
        when(userMapper.selectByUsername("user_a")).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);
        when(jwtUtil.createToken(10, "user")).thenReturn("jwt-token");

        CustomResponse resp = service.login("user_a", "123456");
        assertEquals(200, resp.getCode());
        assertEquals("登录成功", resp.getMessage());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals("jwt-token", data.get("token"));
        verify(userService).ensurePublisherRole(user);
    }

    @Test
    void adminLogin_whenNotAdmin_returns403() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setRole(1);
        user.setPassword("hashed");
        when(userMapper.selectByUsername("user_a")).thenReturn(user);
        when(passwordEncoder.matches("123456", "hashed")).thenReturn(true);

        CustomResponse resp = service.adminLogin("user_a", "123456");
        assertEquals(403, resp.getCode());
        assertEquals("您不是管理员，无权访问", resp.getMessage());
    }

    @Test
    void adminLogin_whenAdmin_returnsToken() {
        User user = new User();
        user.setId(11);
        user.setUsername("admin_test");
        user.setNickname("用户_admin_test");
        user.setRole(2);
        user.setPassword("hashed");
        when(userMapper.selectByUsername("admin_test")).thenReturn(user);
        when(passwordEncoder.matches("admin123", "hashed")).thenReturn(true);
        when(jwtUtil.createToken(11, "admin")).thenReturn("admin-jwt");

        CustomResponse resp = service.adminLogin("admin_test", "admin123");
        assertEquals(200, resp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals("admin-jwt", data.get("token"));
    }

    @Test
    void personalInfo_whenMissing_returns404() {
        when(userService.getUserById(10)).thenReturn(null);
        CustomResponse resp = service.personalInfo(10);
        assertEquals(404, resp.getCode());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void personalInfo_whenExists_returns200() {
        when(userService.getUserById(10)).thenReturn(new UserDTO());
        CustomResponse resp = service.personalInfo(10);
        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
    }

    @Test
    void logout_returns200() {
        CustomResponse resp = service.logout();
        assertEquals(200, resp.getCode());
        assertEquals("已退出登录", resp.getMessage());
    }

    @Test
    void updatePassword_whenNewPasswordBlank_returns500() {
        CustomResponse resp = service.updatePassword(10, "old", "  ");
        assertEquals(500, resp.getCode());
        assertEquals("新密码不能为空", resp.getMessage());
    }

    @Test
    void updatePassword_whenOldPasswordWrong_returns403() {
        User user = new User();
        user.setId(10);
        user.setPassword("hashed");
        when(userMapper.selectById(10)).thenReturn(user);
        when(passwordEncoder.matches("wrong_old", "hashed")).thenReturn(false);
        CustomResponse resp = service.updatePassword(10, "wrong_old", "newpass");
        assertEquals(403, resp.getCode());
        assertEquals("旧密码不正确", resp.getMessage());
    }

    @Test
    void updatePassword_whenSamePassword_returns500() {
        User user = new User();
        user.setId(10);
        user.setPassword("hashed");
        when(userMapper.selectById(10)).thenReturn(user);
        when(passwordEncoder.matches("same", "hashed")).thenReturn(true);
        CustomResponse resp = service.updatePassword(10, "same", "same");
        assertEquals(500, resp.getCode());
        assertEquals("新密码不能与旧密码相同", resp.getMessage());
    }

    @Test
    void updatePassword_whenValid_returns200() {
        User user = new User();
        user.setId(10);
        user.setPassword("hashed");
        when(userMapper.selectById(10)).thenReturn(user);
        when(passwordEncoder.matches("oldpass", "hashed")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("new-hashed");
        CustomResponse resp = service.updatePassword(10, "oldpass", "newpass");
        assertEquals(200, resp.getCode());
        assertEquals("密码修改成功，请重新登录", resp.getMessage());
        verify(userMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void adminPersonalInfo_whenNotAdmin_returns403() {
        CustomResponse resp = service.adminPersonalInfo(10, false);
        assertEquals(403, resp.getCode());
        assertEquals("您不是管理员，无权访问", resp.getMessage());
    }

    @Test
    void adminPersonalInfo_whenAdmin_returns200() {
        when(userService.getUserById(10)).thenReturn(new UserDTO());
        CustomResponse resp = service.adminPersonalInfo(10, true);
        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
    }
}
