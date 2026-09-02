package com.doinb.gateway.search;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.SearchResultDTO;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.dto.VideoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchServiceImplTest {

    private ServiceClient serviceClient;
    private SearchServiceImpl service;

    @BeforeEach
    void setUp() {
        serviceClient = mock(ServiceClient.class);
        DoinbProperties properties = new DoinbProperties();
        properties.getServices().setUser("http://user");
        properties.getServices().setVideo("http://video");
        properties.getServices().setLive("http://live");
        service = new SearchServiceImpl(serviceClient, properties);
    }

    @Test
    void search_whenKeywordBlank_returnsEmptyListsWithoutCallingDownstream() {
        SearchResultDTO result = service.search("  ", 10, 10, 10);

        assertTrue(result.getVideos().isEmpty());
        assertTrue(result.getUsers().isEmpty());
        assertTrue(result.getLiveRooms().isEmpty());
        verify(serviceClient, never()).get(anyString(), anyString());
    }

    @Test
    void search_whenNoMatch_returnsEmptyLists() {
        when(serviceClient.get(anyString(), anyString())).thenReturn(CustomResponse.ok(List.of()));

        SearchResultDTO result = service.search("xyznotexist123", 10, 10, 10);

        assertTrue(result.getVideos().isEmpty());
        assertTrue(result.getUsers().isEmpty());
        assertTrue(result.getLiveRooms().isEmpty());
    }

    @Test
    void search_whenDownstreamFails_returnsEmptyForThatBucket() {
        VideoDTO video = new VideoDTO();
        video.setId(12);
        video.setTitle("测试视频");
        UserDTO user = new UserDTO();
        user.setId(10);
        user.setNickname("测试昵称");
        when(serviceClient.get(eq("http://video"), startsWith(InternalPaths.SEARCH_VIDEOS)))
                .thenReturn(CustomResponse.ok(List.of(video)));
        when(serviceClient.get(eq("http://user"), startsWith(InternalPaths.SEARCH_USERS)))
                .thenReturn(CustomResponse.ok(List.of(user)));
        when(serviceClient.get(eq("http://live"), startsWith(InternalPaths.SEARCH_LIVES)))
                .thenReturn(CustomResponse.fail(502, "调用下游失败"));

        SearchResultDTO result = service.search("测试", 10, 10, 10);

        assertEquals(1, result.getVideos().size());
        assertEquals(12, result.getVideos().get(0).getId());
        assertEquals(1, result.getUsers().size());
        assertEquals("测试昵称", result.getUsers().get(0).getNickname());
        assertTrue(result.getLiveRooms().isEmpty());
        assertEquals(1, result.getNotices().size());
        assertTrue(result.getNotices().get(0).contains("直播"));
    }

    @Test
    void search_whenVideoDown_keepsUsersAndAddsNotice() {
        UserDTO user = new UserDTO();
        user.setId(10);
        user.setNickname("演示观众");
        when(serviceClient.get(eq("http://video"), startsWith(InternalPaths.SEARCH_VIDEOS)))
                .thenReturn(CustomResponse.fail(502, "调用下游失败"));
        when(serviceClient.get(eq("http://user"), startsWith(InternalPaths.SEARCH_USERS)))
                .thenReturn(CustomResponse.ok(List.of(user)));
        when(serviceClient.get(eq("http://live"), startsWith(InternalPaths.SEARCH_LIVES)))
                .thenReturn(CustomResponse.ok(List.of()));

        SearchResultDTO result = service.search("演示", 10, 10, 10);

        assertTrue(result.getVideos().isEmpty());
        assertEquals(1, result.getUsers().size());
        assertTrue(result.getNotices().get(0).contains("视频"));
        assertTrue(result.getNotices().stream().noneMatch(n -> n.contains("用户")));
    }
}
