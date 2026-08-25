package com.doinb.backend.api;

import com.doinb.backend.controller.SearchController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.dto.SearchResultDTO;
import com.doinb.backend.service.search.SearchService;
import com.doinb.backend.service.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 对应测试报告 S001：公开搜索，无 Token，无结果仍 200 */
@WebMvcTest(controllers = SearchController.class)
@ImportApiSecurity
class SearchApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void search_whenNoMatch_returnsEmptyLists() throws Exception {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setVideos(List.of());
        dto.setUsers(List.of());
        dto.setLiveRooms(List.of());
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
}
