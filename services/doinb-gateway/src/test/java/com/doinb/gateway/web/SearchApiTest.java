package com.doinb.gateway.web;

import com.doinb.common.dto.SearchResultDTO;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.dto.VideoDTO;
import com.doinb.common.web.HealthController;
import com.doinb.common.config.DoinbProperties;
import com.doinb.gateway.search.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SearchApiTest {

    private SearchService searchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        DoinbProperties properties = new DoinbProperties();
        properties.setServiceName("doinb-gateway");
        mockMvc = standaloneSetup(new SearchController(searchService), new HealthController(properties, null, "dev")).build();
    }

    @Test
    void search_whenNoMatch_returnsEmptyLists() throws Exception {
        SearchResultDTO dto = new SearchResultDTO();
        when(searchService.search(eq("xyznotexist123"), eq(10L), eq(10L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/search")
                        .param("keyword", "xyznotexist123")
                        .param("videoLimit", "10")
                        .param("liveLimit", "10")
                        .param("userLimit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.videos").isEmpty())
                .andExpect(jsonPath("$.data.users").isEmpty())
                .andExpect(jsonPath("$.data.liveRooms").isEmpty());
    }

    @Test
    void search_withResults_keepsMonolithShape() throws Exception {
        SearchResultDTO dto = new SearchResultDTO();
        VideoDTO video = new VideoDTO();
        video.setId(12);
        video.setTitle("测试视频");
        UserDTO user = new UserDTO();
        user.setId(10);
        user.setNickname("测试昵称");
        dto.setVideos(List.of(video));
        dto.setUsers(List.of(user));
        when(searchService.search(eq("测试"), eq(10L), eq(10L), eq(10L))).thenReturn(dto);

        mockMvc.perform(get("/search").param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.videos[0].title").value("测试视频"))
                .andExpect(jsonPath("$.data.users[0].nickname").value("测试昵称"))
                .andExpect(jsonPath("$.data.liveRooms").isEmpty());
    }

    @Test
    void health_returnsGatewayName() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("doinb-gateway ok"));
        mockMvc.perform(get("/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("ready"));
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.service").value("doinb-gateway"))
                .andExpect(jsonPath("$.data.version").value("dev"));
    }
}
