package com.doinb.backend.service.search.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.config.LiveStreamHelper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.dto.SearchResultDTO;
import com.doinb.backend.pojo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 对应测试报告 S000 / S001 / S002 */
class SearchServiceImplTest {

    private VideoMapper videoMapper;
    private LiveRoomMapper liveRoomMapper;
    private UserMapper userMapper;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        liveRoomMapper = mock(LiveRoomMapper.class);
        userMapper = mock(UserMapper.class);
        service = new SearchServiceImpl(
                videoMapper,
                liveRoomMapper,
                userMapper,
                mock(LiveStreamHelper.class)
        );
    }

    @Test
    void search_whenKeywordBlank_returnsEmptyLists() {
        SearchResultDTO result = service.search("  ", 10, 10, 10);

        assertNotNull(result.getVideos());
        assertNotNull(result.getUsers());
        assertNotNull(result.getLiveRooms());
        assertTrue(result.getVideos().isEmpty());
        assertTrue(result.getUsers().isEmpty());
        assertTrue(result.getLiveRooms().isEmpty());
    }

    @Test
    void search_whenNoMatch_returnsEmptyLists() {
        when(videoMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(liveRoomMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        SearchResultDTO result = service.search("xyznotexist123", 10, 10, 10);

        assertTrue(result.getVideos().isEmpty());
        assertTrue(result.getUsers().isEmpty());
        assertTrue(result.getLiveRooms().isEmpty());
    }

    @Test
    void search_whenUserMatches_returnsUsers() {
        User user = new User();
        user.setId(10);
        user.setUsername("user_a");
        user.setNickname("测试昵称");
        user.setRole(1);
        when(videoMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(liveRoomMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of(user));

        SearchResultDTO result = service.search("测试", 10, 10, 10);

        assertTrue(result.getVideos().isEmpty());
        assertTrue(result.getLiveRooms().isEmpty());
        assertEquals(1, result.getUsers().size());
        assertEquals(10, result.getUsers().get(0).getId());
        assertEquals("测试昵称", result.getUsers().get(0).getNickname());
    }
}
