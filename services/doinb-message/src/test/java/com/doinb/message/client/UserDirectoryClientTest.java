package com.doinb.message.client;

import com.doinb.common.CustomResponse;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.UserDTO;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDirectoryClientTest {

    @Test
    void findByIds_convertsInternalResponseWithoutReadingUsersTable() {
        ServiceClient serviceClient = mock(ServiceClient.class);
        DoinbProperties properties = new DoinbProperties();
        properties.getServices().setUser("http://user:8082");
        UserDirectoryClient client = new UserDirectoryClient(serviceClient, properties);
        when(serviceClient.get(eq("http://user:8082"), contains("ids=7%2C8")))
                .thenReturn(CustomResponse.ok(List.of(
                        Map.of("id", 7, "nickname", "用户七", "avatar", "/7.png"),
                        Map.of("id", 8, "nickname", "用户八", "avatar", "/8.png"))));

        Map<Integer, UserDTO> users = client.findByIds(List.of(7, 8));

        assertEquals("用户七", users.get(7).getNickname());
        assertEquals("/8.png", users.get(8).getAvatar());
    }

    @Test
    void findByIds_whenDownstreamFails_returnsEmptyMap() {
        ServiceClient serviceClient = mock(ServiceClient.class);
        DoinbProperties properties = new DoinbProperties();
        UserDirectoryClient client = new UserDirectoryClient(serviceClient, properties);
        when(serviceClient.get(anyString(), anyString()))
                .thenReturn(CustomResponse.fail(502, "调用下游失败"));

        assertTrue(client.findByIds(List.of(7)).isEmpty());
    }

}
