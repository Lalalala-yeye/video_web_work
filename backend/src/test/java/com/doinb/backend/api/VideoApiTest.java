package com.doinb.backend.api;

import com.doinb.backend.controller.VideoController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.video.VideoService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 对应测试报告 V011（公开详情 404）/ V031（已登录但缺 file → HTTP 500） */
@WebMvcTest(controllers = VideoController.class)
@ImportApiSecurity
class VideoApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private VideoService videoService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void getOne_whenMissing_returns404() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setCode(404);
        body.setMessage("视频不存在或未发布");
        when(videoService.getOne(eq(99999), nullable(Integer.class), nullable(Integer.class))).thenReturn(body);

        mockMvc.perform(get("/video/getone").param("id", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("视频不存在或未发布"));
    }

    @Test
    void upload_withoutFile_returnsHttp500() throws Exception {
        User user = new User();
        user.setId(10);
        user.setRole(1);
        when(userMapper.selectById(10)).thenReturn(user);

        String token = jwtUtil.createToken(10, "user");
        mockMvc.perform(multipart("/video/upload")
                        .param("title", "测试视频标题")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value(containsString("file")));
    }
}
