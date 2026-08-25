package com.doinb.backend.service.users.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.utils.CurrentUser;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 U000 / U001 / U002 / U010 / U011 / U020 / U030 / U040 */
class UserAccountServiceImplTest {

    private UserMapper userMapper;
    private UserService userService;
    private PasswordEncoder passwordEncoder;
    private AuthenticationProvider authenticationProvider;
    private JwtUtil jwtUtil;
    private CurrentUser currentUser;
    private UserAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        userService = mock(UserService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authenticationProvider = mock(AuthenticationProvider.class);
        jwtUtil = mock(JwtUtil.class);
        currentUser = mock(CurrentUser.class);
        service = new UserAccountServiceImpl(
                userMapper,
                userService,
                passwordEncoder,
                authenticationProvider,
                jwtUtil,
                currentUser
        );
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
        when(authenticationProvider.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        CustomResponse resp = service.login("user_a", "wrong_pass");

        assertEquals(403, resp.getCode());
        assertEquals("账号或密码不正确", resp.getMessage());
    }

    @Test
    void login_whenValid_returnsToken() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setNickname("用户_user_a");
        user.setRole(1);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UserDetailsImpl(user));
        when(authenticationProvider.authenticate(any())).thenReturn(auth);
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
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UserDetailsImpl(user));
        when(authenticationProvider.authenticate(any())).thenReturn(auth);

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
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UserDetailsImpl(user));
        when(authenticationProvider.authenticate(any())).thenReturn(auth);
        when(jwtUtil.createToken(11, "admin")).thenReturn("admin-jwt");

        CustomResponse resp = service.adminLogin("admin_test", "admin123");

        assertEquals(200, resp.getCode());
        assertEquals("登录成功", resp.getMessage());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals("admin-jwt", data.get("token"));
    }

    @Test
    void personalInfo_whenMissing_returns404() {
        when(currentUser.getUserId()).thenReturn(10);
        when(userService.getUserById(10)).thenReturn(null);

        CustomResponse resp = service.personalInfo();

        assertEquals(404, resp.getCode());
        assertEquals("用户不存在", resp.getMessage());
    }

    @Test
    void personalInfo_whenExists_returns200() {
        when(currentUser.getUserId()).thenReturn(10);
        when(userService.getUserById(10)).thenReturn(new UserDTO());

        CustomResponse resp = service.personalInfo();

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
    void register_whenPasswordBlank_returns403() {
        CustomResponse resp = service.register("user_a", "  ", "  ");

        assertEquals(403, resp.getCode());
        assertEquals("密码不能为空", resp.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_whenUsernameTooLong_returns403() {
        String longName = "a".repeat(51);

        CustomResponse resp = service.register(longName, "123456", "123456");

        assertEquals(403, resp.getCode());
        assertEquals("账号长度不能超过50", resp.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void register_whenPasswordTooLong_returns403() {
        String longPwd = "a".repeat(51);

        CustomResponse resp = service.register("user_a", longPwd, longPwd);

        assertEquals(403, resp.getCode());
        assertEquals("密码长度不能超过50", resp.getMessage());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void updatePassword_whenNewPasswordBlank_returns500() {
        CustomResponse resp = service.updatePassword("old", "  ");

        assertEquals(500, resp.getCode());
        assertEquals("新密码不能为空", resp.getMessage());
    }

    @Test
    void updatePassword_whenNewPasswordTooLong_returns500() {
        String longPwd = "a".repeat(51);

        CustomResponse resp = service.updatePassword("old", longPwd);

        assertEquals(500, resp.getCode());
        assertEquals("新密码长度不能超过50", resp.getMessage());
    }

    @Test
    void updatePassword_whenOldPasswordWrong_returns403() {
        setLoginUser(new User());
        when(authenticationProvider.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        CustomResponse resp = service.updatePassword("wrong_old", "newpass");

        assertEquals(403, resp.getCode());
        assertEquals("旧密码不正确", resp.getMessage());
    }

    @Test
    void updatePassword_whenSamePassword_returns500() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        setLoginUser(user);
        when(authenticationProvider.authenticate(any())).thenReturn(mock(Authentication.class));

        CustomResponse resp = service.updatePassword("same", "same");

        assertEquals(500, resp.getCode());
        assertEquals("新密码不能与旧密码相同", resp.getMessage());
    }

    @Test
    void updatePassword_whenValid_returns200() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        setLoginUser(user);
        when(authenticationProvider.authenticate(any())).thenReturn(mock(Authentication.class));
        when(passwordEncoder.encode("newpass")).thenReturn("hashed");

        CustomResponse resp = service.updatePassword("oldpass", "newpass");

        assertEquals(200, resp.getCode());
        assertEquals("密码修改成功，请重新登录", resp.getMessage());
        verify(userMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void adminPersonalInfo_whenNotAdmin_returns403() {
        when(currentUser.isAdmin()).thenReturn(false);

        CustomResponse resp = service.adminPersonalInfo();

        assertEquals(403, resp.getCode());
        assertEquals("您不是管理员，无权访问", resp.getMessage());
    }

    @Test
    void adminPersonalInfo_whenAdmin_returns200() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(currentUser.getUserId()).thenReturn(10);
        when(userService.getUserById(10)).thenReturn(new UserDTO());

        CustomResponse resp = service.adminPersonalInfo();

        assertEquals(200, resp.getCode());
        assertEquals("OK", resp.getMessage());
    }

    private void setLoginUser(User user) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(new UserDetailsImpl(user));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
