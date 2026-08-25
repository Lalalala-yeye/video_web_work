package com.doinb.backend.service.subscription.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.config.LiveStreamHelper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.SubscriptionMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.FeedItemDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.Subscription;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 F000 / F001 / F010 */
class SubscriptionServiceImplTest {

    private SubscriptionMapper subscriptionMapper;
    private UserMapper userMapper;
    private VideoMapper videoMapper;
    private LiveRoomMapper liveRoomMapper;
    private SubscriptionServiceImpl service;

    @BeforeEach
    void setUp() {
        subscriptionMapper = mock(SubscriptionMapper.class);
        userMapper = mock(UserMapper.class);
        videoMapper = mock(VideoMapper.class);
        liveRoomMapper = mock(LiveRoomMapper.class);
        service = new SubscriptionServiceImpl(
                subscriptionMapper,
                userMapper,
                videoMapper,
                liveRoomMapper,
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

    @Test
    void isFollowing_whenNullArgs_returnsFalse() {
        assertEquals(false, service.isFollowing(null, 11));
        assertEquals(false, service.isFollowing(10, null));
    }

    @Test
    void isFollowing_whenCountPositive_returnsTrue() {
        when(subscriptionMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertEquals(true, service.isFollowing(10, 11));
    }

    @Test
    void listFollowing_whenEmpty_returnsEmptyPage() {
        when(subscriptionMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Subscription> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<UserDTO> result = service.listFollowing(10, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void listFollowing_whenHasSubs_returnsUsers() {
        Subscription sub = new Subscription();
        sub.setFollowerId(10);
        sub.setTargetId(11);
        when(subscriptionMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Subscription> page = invocation.getArgument(0);
            page.setRecords(List.of(sub));
            page.setTotal(1);
            return page;
        });
        User target = new User();
        target.setId(11);
        target.setNickname("目标用户");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(target));

        PageResult<UserDTO> result = service.listFollowing(10, 1, 10);

        assertEquals(1, result.getRecords().size());
        assertEquals("目标用户", result.getRecords().get(0).getNickname());
    }

    @Test
    void feed_whenNoSubs_returnsEmptyPage() {
        when(subscriptionMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        PageResult<FeedItemDTO> result = service.feed(10, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void feed_whenHasSubs_returnsItems() {
        Subscription sub = new Subscription();
        sub.setFollowerId(10);
        sub.setTargetId(11);
        when(subscriptionMapper.selectList(any(Wrapper.class))).thenReturn(List.of(sub));

        Video video = new Video();
        video.setId(1);
        video.setAuthorId(11);
        video.setTitle("视频");
        video.setCreateTime(LocalDateTime.now());
        when(videoMapper.selectList(any(Wrapper.class))).thenReturn(List.of(video));

        LiveRoom room = new LiveRoom();
        room.setId(2);
        room.setAnchorId(11);
        room.setIsLive(true);
        when(liveRoomMapper.selectList(any(Wrapper.class))).thenReturn(List.of(room));

        User author = new User();
        author.setId(11);
        author.setNickname("作者");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(author));

        PageResult<FeedItemDTO> result = service.feed(10, 1, 10);

        assertEquals(2, result.getTotal());
        assertEquals(2, result.getRecords().size());
    }
}
