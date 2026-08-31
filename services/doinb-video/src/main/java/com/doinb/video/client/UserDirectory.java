package com.doinb.video.client;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 只通过用户服务内部接口取昵称，不查 users 表。下游挂了则当「未知作者」。 */
@Component
public class UserDirectory {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public UserDirectory(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    public UserDTO findById(Integer id) {
        if (id == null) {
            return null;
        }
        return mapByIds(List.of(id)).get(id);
    }

    public Map<Integer, UserDTO> mapByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        String joined = ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (joined.isEmpty()) {
            return Map.of();
        }
        try {
            CustomResponse resp = serviceClient.get(
                    properties.getServices().getUser(),
                    InternalPaths.USERS + "?ids=" + joined);
            if (resp.getCode() != 200 || resp.getData() == null) {
                return Map.of();
            }
            return toMap(resp.getData());
        } catch (RuntimeException ex) {
            return Map.of();
        }
    }

    private static Map<Integer, UserDTO> toMap(Object data) {
        if (!(data instanceof List<?> list)) {
            return Map.of();
        }
        Map<Integer, UserDTO> map = new HashMap<>();
        for (Object item : list) {
            UserDTO user = toUser(item);
            if (user != null && user.getId() != null) {
                map.put(user.getId(), user);
            }
        }
        return map;
    }

    private static UserDTO toUser(Object item) {
        if (item instanceof UserDTO dto) {
            return dto;
        }
        if (!(item instanceof Map<?, ?> raw)) {
            return null;
        }
        UserDTO dto = new UserDTO();
        dto.setId(asInteger(raw.get("id")));
        dto.setUsername(asString(raw.get("username")));
        dto.setNickname(asString(raw.get("nickname")));
        dto.setAvatar(asString(raw.get("avatar")));
        dto.setRole(asInteger(raw.get("role")));
        dto.setBio(asString(raw.get("bio")));
        return dto;
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s && !s.isBlank()) {
            return Integer.valueOf(s);
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
