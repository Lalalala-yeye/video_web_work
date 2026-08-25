package com.doinb.backend.service.search.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.doinb.backend.config.LiveStreamHelper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.dto.SearchResultDTO;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
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
    private LiveStreamHelper liveStreamHelper;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        videoMapper = mock(VideoMapper.class);
        liveRoomMapper = mock(LiveRoomMapper.class);
        userMapper = mock(UserMapper.class);
        liveStreamHelper = mock(LiveStreamHelper.class);
        service = new SearchServiceImpl(
                videoMapper,
                liveRoomMapper,
                userMapper,
                liveStreamHelper
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

    @Test
    void search_whenVideoMatches_returnsVideos() {
        Video video = new Video();
        video.setId(1);
        video.setTitle("测试视频");
        video.setAuthorId(10);
        when(videoMapper.selectList(any(Wrapper.class))).thenReturn(List.of(video));
        when(liveRoomMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        User author = new User();
        author.setId(10);
        author.setNickname("作者");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(author));

        SearchResultDTO result = service.search("测试", 10, 10, 10);

        assertTrue(result.getLiveRooms().isEmpty());
        assertEquals(1, result.getVideos().size());
        assertEquals("作者", result.getVideos().get(0).getAuthorNickname());
    }

    @Test
    void search_whenLiveMatches_returnsLiveRooms() {
        when(videoMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        LiveRoom room = new LiveRoom();
        room.setId(1);
        room.setAnchorId(10);
        room.setIsLive(true);
        room.setStreamKey("abc");
        when(liveRoomMapper.selectList(any(Wrapper.class))).thenReturn(List.of(room));
        when(userMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        User anchor = new User();
        anchor.setId(10);
        anchor.setNickname("主播");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(anchor));
        when(liveStreamHelper.playUrl("abc")).thenReturn("/live/abc.m3u8");

        SearchResultDTO result = service.search("测试", 10, 10, 10);

        assertTrue(result.getVideos().isEmpty());
        assertEquals(1, result.getLiveRooms().size());
        assertEquals("主播", result.getLiveRooms().get(0).getAnchorNickname());
        assertEquals("/live/abc.m3u8", result.getLiveRooms().get(0).getPlayUrl());
    }
}
