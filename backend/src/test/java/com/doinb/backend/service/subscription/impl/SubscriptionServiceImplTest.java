package com.doinb.backend.service.subscription.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.config.LiveStreamHelper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.SubscriptionMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.Subscription;
import com.doinb.backend.pojo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 F000 / F001 / F010 */
class SubscriptionServiceImplTest {

    private SubscriptionMapper subscriptionMapper;
    private UserMapper userMapper;
    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        subscriptionMapper = mock(SubscriptionMapper.class);
        userMapper = mock(UserMapper.class);
        service = new SubscriptionServiceImpl(
                subscriptionMapper,
                userMapper,
                mock(VideoMapper.class),
                mock(LiveRoomMapper.class),
                mock(LiveStreamHelper.class)
        );
    }

    @Test
    void follow_whenSelf_returns400() {
        CustomResponse resp = service.follow(10, 10);

        assertEquals(400, resp.getCode());
        assertEquals("不能关注自己", resp.getMessage());
        verify(subscriptionMapper, never()).insert(any(Subscription.class));
    }

    @Test
    void follow_whenTargetMissing_returns404() {
        when(userMapper.selectById(11)).thenReturn(null);

        CustomResponse resp = service.follow(10, 11);

        assertEquals(404, resp.getCode());
        assertEquals("目标用户不存在", resp.getMessage());
    }

    @Test
    void follow_whenNew_returns200() {
        when(userMapper.selectById(11)).thenReturn(new User());
        when(subscriptionMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        CustomResponse resp = service.follow(10, 11);

        assertEquals(200, resp.getCode());
        assertEquals("关注成功", resp.getMessage());
        verify(subscriptionMapper).insert(any(Subscription.class));
    }

    @Test
    void unfollow_whenExisted_returns200() {
        when(subscriptionMapper.delete(any(Wrapper.class))).thenReturn(1);

        CustomResponse resp = service.unfollow(10, 11);

        assertEquals(200, resp.getCode());
        assertEquals("已取消关注", resp.getMessage());
    }

    @Test
    void follow_whenAlreadyFollowing_returns200() {
        when(userMapper.selectById(11)).thenReturn(new User());
        when(subscriptionMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        CustomResponse resp = service.follow(10, 11);

        assertEquals(200, resp.getCode());
        assertEquals("已关注", resp.getMessage());
        verify(subscriptionMapper, never()).insert(any(Subscription.class));
    }

    @Test
    void unfollow_whenNotFollowing_returns200() {
        when(subscriptionMapper.delete(any(Wrapper.class))).thenReturn(0);

        CustomResponse resp = service.unfollow(10, 11);

        assertEquals(200, resp.getCode());
        assertEquals("未关注", resp.getMessage());
    }

    @Test
    void isFollowing_whenCountZero_returnsFalse() {
        when(subscriptionMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        assertEquals(false, service.isFollowing(10, 11));
    }
}
