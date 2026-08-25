package com.doinb.backend.service.users.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserPublicDTO;
import com.doinb.backend.pojo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 U060 / U061 / U080 / U081 */
class UserServiceImplTest {

    private UserMapper userMapper;
    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        service = new UserServiceImpl(userMapper, mock(VideoMapper.class));
    }

    @Test
    void getPublicProfile_whenMissing_returnsNull() {
        when(userMapper.selectById(99999)).thenReturn(null);

        assertNull(service.getPublicProfile(99999));
    }

    @Test
    void getPublicProfile_whenExists_returnsPublicFields() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setNickname("用户_user_a");
        user.setAvatar("avatar.png");
        user.setBio(null);
        when(userMapper.selectById(10)).thenReturn(user);

        UserPublicDTO dto = service.getPublicProfile(10);

        assertNotNull(dto);
        assertEquals(10, dto.getId());
        assertEquals("用户_user_a", dto.getNickname());
        assertEquals("avatar.png", dto.getAvatar());
        assertNull(dto.getBio());
    }

    @Test
    void updateUserInfo_whenNicknameBlank_returns500() {
        CustomResponse resp = service.updateUserInfo(10, "  ", "这是简介");

        assertEquals(500, resp.getCode());
        assertEquals("昵称不能为空", resp.getMessage());
        verify(userMapper, never()).update(isNull(), any(Wrapper.class));
    }

    @Test
    void updateUserInfo_whenValid_returns200() {
        CustomResponse resp = service.updateUserInfo(10, "测试昵称", "这是简介");

        assertEquals(200, resp.getCode());
        assertEquals("资料更新成功", resp.getMessage());
        verify(userMapper).update(isNull(), any(Wrapper.class));
    }

    @Test
    void uploadAvatar_whenFileMissing_returns400() {
        CustomResponse resp = service.uploadAvatar(10, null);

        assertEquals(400, resp.getCode());
        assertEquals("请选择头像图片", resp.getMessage());
    }

    @Test
    void uploadAvatar_whenFormatInvalid_returns400() {
        MockMultipartFile file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        CustomResponse resp = service.uploadAvatar(10, file);

        assertEquals(400, resp.getCode());
        assertEquals("头像格式仅支持 jpg / png / webp / gif", resp.getMessage());
    }
}
