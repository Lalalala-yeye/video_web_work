package com.doinb.backend.api;

import com.doinb.backend.controller.HealthController;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.service.users.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 示例：公开接口，无 Token 也应 200。对应测试报告 H000。
 * <p>
 * 复制本类时改 {@code controllers} 和请求即可。不要用 {@code @WithMockUser}。
 */
@WebMvcTest(controllers = HealthController.class)
@ImportApiSecurity
class HealthApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserMapper userMapper;
    @MockitoBean
    private UserService userService;

    @Test
    void health_withoutToken_returns200() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("doinb-backend ok"));
    }
}
