package com.doinb.backend.api;

import com.doinb.backend.controller.LiveRoomController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.LiveRoomDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.live.LiveRoomService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 直播接口的基础访问控制与参数转发测试。 */
@WebMvcTest(controllers = LiveRoomController.class)
@ImportApiSecurity
class LiveRoomApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private LiveRoomService liveRoomService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void list_withoutToken_returnsDefaultPage() throws Exception {
        when(liveRoomService.list(1, 12))
                .thenReturn(new PageResult<LiveRoomDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/live/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(12))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(liveRoomService).list(1, 12);
    }

    @Test
    void create_withoutToken_returnsHttp403() throws Exception {
        mockMvc.perform(post("/live/create").param("title", "测试直播间"))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withToken_forwardsCurrentUser() throws Exception {
        User user = user(10, 1);
        when(userMapper.selectById(10)).thenReturn(user);
        CustomResponse body = new CustomResponse();
        body.setMessage("创建成功");
        when(liveRoomService.create(10, 1, "测试直播间")).thenReturn(body);

        mockMvc.perform(post("/live/create")
                        .param("title", "测试直播间")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"));

        verify(liveRoomService).create(10, 1, "测试直播间");
    }

    @Test
    void getOne_withoutToken_returnsServiceBody() throws Exception {
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(liveRoomService.getOne(12, null, null)).thenReturn(body);

        mockMvc.perform(get("/live/getone").param("id", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(liveRoomService).getOne(12, null, null);
    }

    @Test
    void getOne_withToken_forwardsViewerIdentity() throws Exception {
        when(userMapper.selectById(10)).thenReturn(user(10, 1));
        CustomResponse body = new CustomResponse();
        body.setMessage("OK");
        when(liveRoomService.getOne(12, 10, 1)).thenReturn(body);

        mockMvc.perform(get("/live/getone")
                        .param("id", "12")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(liveRoomService).getOne(12, 10, 1);
    }

    @Test
    void start_withToken_forwardsCurrentUser() throws Exception {
        when(userMapper.selectById(10)).thenReturn(user(10, 1));
        CustomResponse body = new CustomResponse();
        body.setMessage("开播成功");
        when(liveRoomService.startLive(10, 1, 12)).thenReturn(body);

        mockMvc.perform(post("/live/start")
                        .param("id", "12")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("开播成功"));

        verify(liveRoomService).startLive(10, 1, 12);
    }

    @Test
    void stop_withToken_forwardsCurrentUser() throws Exception {
        when(userMapper.selectById(10)).thenReturn(user(10, 1));
        CustomResponse body = new CustomResponse();
        body.setMessage("停播成功");
        when(liveRoomService.stopLive(10, 1, 12)).thenReturn(body);

        mockMvc.perform(post("/live/stop")
                        .param("id", "12")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("停播成功"));

        verify(liveRoomService).stopLive(10, 1, 12);
    }

    @Test
    void myList_withToken_returnsDefaultPage() throws Exception {
        when(userMapper.selectById(10)).thenReturn(user(10, 1));
        when(liveRoomService.listMyRooms(10, 1, 12))
                .thenReturn(new PageResult<LiveRoomDTO>(0, 1, 12, List.of()));

        mockMvc.perform(get("/live/my/list")
                        .header("Authorization", "Bearer " + jwtUtil.createToken(10, "user")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(liveRoomService).listMyRooms(10, 1, 12);
    }

    private User user(Integer id, Integer role) {
        User user = new User();
        user.setId(id);
        user.setUsername("user_" + id);
        user.setRole(role);
        return user;
    }
}
