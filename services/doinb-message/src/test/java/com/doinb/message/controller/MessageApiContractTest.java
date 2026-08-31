package com.doinb.message.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.GatewayHeaders;
import com.doinb.common.PageResult;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.CreateNotificationRequest;
import com.doinb.common.web.DownstreamAuthFilter;
import com.doinb.message.internal.InternalNotificationController;
import com.doinb.message.pojo.dto.NotificationDTO;
import com.doinb.message.service.MessageService;
import com.doinb.message.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class MessageApiContractTest {

    private NotificationService notificationService;
    private MessageService messageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        notificationService = mock(NotificationService.class);
        messageService = mock(MessageService.class);
        DoinbProperties properties = new DoinbProperties();
        properties.setRole("service");
        properties.setInternalToken("test-internal-token");
        properties.setPublicPathPrefixes(List.of("/health"));
        mockMvc = standaloneSetup(
                new NotificationController(notificationService),
                new MessageController(messageService),
                new InternalNotificationController(notificationService))
                .addFilters(new DownstreamAuthFilter(properties))
                .build();
    }

    @Test
    void externalEndpoints_withoutUserHeader_return403() throws Exception {
        mockMvc.perform(get("/notification/list"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/message/send").param("roomId", "20").param("content", "你好"))
                .andExpect(status().isForbidden());
    }

    @Test
    void notificationEndpoints_keepPathsParametersAndResponseShape() throws Exception {
        NotificationDTO row = new NotificationDTO();
        row.setId(40);
        row.setPreview("收到点赞");
        when(notificationService.list(10, 1, 20))
                .thenReturn(new PageResult<>(1, 1, 20, List.of(row)));
        when(notificationService.countUnread(10)).thenReturn(3L);
        when(notificationService.markRead(10, 40))
                .thenReturn(CustomResponse.ok("已标为已读", null));

        mockMvc.perform(get("/notification/list").header(GatewayHeaders.USER_ID, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].preview").value("收到点赞"));
        mockMvc.perform(get("/notification/unread-count").header(GatewayHeaders.USER_ID, "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.count").value(3));
        mockMvc.perform(post("/notification/read")
                        .header(GatewayHeaders.USER_ID, "10").param("id", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("已标为已读"));
    }

    @Test
    void messageEndpoints_keepPathsAndForwardGatewayUser() throws Exception {
        when(messageService.openRoom(10, 11)).thenReturn(CustomResponse.ok());
        when(messageService.getRoom(10, 20, 1, 50)).thenReturn(CustomResponse.ok());
        when(messageService.send(10, 20, "你好")).thenReturn(CustomResponse.ok("发送成功", null));

        mockMvc.perform(post("/message/room/open")
                        .header(GatewayHeaders.USER_ID, "10").param("peerId", "11"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/message/room/get")
                        .header(GatewayHeaders.USER_ID, "10").param("roomId", "20"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/message/send")
                        .header(GatewayHeaders.USER_ID, "10")
                        .param("roomId", "20").param("content", "你好"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("发送成功"));

        verify(messageService).openRoom(10, 11);
        verify(messageService).getRoom(10, 20, 1, 50);
        verify(messageService).send(10, 20, "你好");
    }

    @Test
    void internalNotification_requiresInternalTokenAndUsesContractBody() throws Exception {
        when(notificationService.create(any(CreateNotificationRequest.class)))
                .thenReturn(CustomResponse.ok("通知创建成功", null));
        String body = """
                {"userId":12,"type":4,"actorId":1,"refId":30,
                 "preview":"审核通过","linkPath":"/video/30"}
                """;

        mockMvc.perform(post("/internal/notifications")
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/internal/notifications")
                        .header(GatewayHeaders.INTERNAL_TOKEN, "test-internal-token")
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("通知创建成功"));
    }
}
