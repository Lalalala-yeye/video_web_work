package com.doinb.backend.api;

import com.doinb.backend.controller.UserController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserPublicDTO;
import com.doinb.backend.pojo.dto.UserShowcaseDTO;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.subscription.SubscriptionService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 用户资料接口的公开查询与登录修改测试。 */
@WebMvcTest(controllers = UserController.class)
@ImportApiSecurity
class UserApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private SubscriptionService subscriptionService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;

    @Test
    void getOne_withoutToken_returnsPublicProfile() throws Exception {
        UserPublicDTO profile = new UserPublicDTO();
        profile.setId(20);
        profile.setNickname("公开昵称");
        when(userService.getPublicProfile(20)).thenReturn(profile);

        mockMvc.perform(get("/user/info/get-one").param("uid", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(20))
                .andExpect(jsonPath("$.data.nickname").value("公开昵称"));

        verify(userService).getPublicProfile(20);
    }

    @Test
    void getOne_whenUserMissing_returnsJson404() throws Exception {
        when(userService.getPublicProfile(999)).thenReturn(null);

        mockMvc.perform(get("/user/info/get-one").param("uid", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("用户不存在"));

        verify(userService).getPublicProfile(999);
    }

    @Test
    void updateInfo_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(post("/user/info/update").param("nickname", "新昵称"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateInfo_withToken_forwardsCurrentUser() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("修改成功");
        when(userService.updateUserInfo(10, "新昵称", "个人简介")).thenReturn(body);

        mockMvc.perform(post("/user/info/update")
                        .param("nickname", "新昵称")
                        .param("bio", "个人简介")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("修改成功"));

        verify(userService).updateUserInfo(10, "新昵称", "个人简介");
    }

    @Test
    void showcase_withoutToken_returnsPublicData() throws Exception {
        UserShowcaseDTO showcase = new UserShowcaseDTO();
        showcase.setProfile(new UserPublicDTO());
        showcase.setVideos(java.util.List.of());
        showcase.setVideoTotal(0);
        when(userService.getShowcase(20, 1, 12)).thenReturn(showcase);

        mockMvc.perform(get("/user/profile/showcase").param("uid", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.videoTotal").value(0))
                .andExpect(jsonPath("$.data.videos").isEmpty())
                .andExpect(jsonPath("$.data.following").value(false));

        verify(userService).getShowcase(20, 1, 12);
    }

    @Test
    void uploadAvatar_withToken_forwardsFile() throws Exception {
        User user = user(10);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("上传成功");
        when(userService.uploadAvatar(org.mockito.ArgumentMatchers.eq(10), any(MultipartFile.class)))
                .thenReturn(body);
        MockMultipartFile avatar = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/user/avatar/upload")
                        .file(avatar)
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("上传成功"));

        verify(userService).uploadAvatar(org.mockito.ArgumentMatchers.eq(10), any(MultipartFile.class));
    }

    private User user(Integer id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(1);
        return user;
    }
}
