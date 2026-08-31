package com.doinb.message.client;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.UserDTO;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 只通过用户服务内部接口取得展示信息，不直接访问 users 表。 */
@Component
public class UserDirectoryClient {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public UserDirectoryClient(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    public UserDTO findById(Integer id) {
        if (id == null) {
            return null;
        }
        return findByIds(List.of(id)).get(id);
    }

    public Map<Integer, UserDTO> findByIds(Collection<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        String csv = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (csv.isEmpty()) {
            return Map.of();
        }

        CustomResponse response = serviceClient.get(
                properties.getServices().getUser(),
                ServiceClient.query(InternalPaths.USERS, "ids", csv));
        if (response.getCode() != 200 || !(response.getData() instanceof Collection<?> rows)) {
            return Map.of();
        }

        Map<Integer, UserDTO> result = new LinkedHashMap<>();
        for (Object row : rows) {
            UserDTO user = toUser(row);
            if (user != null && user.getId() != null) {
                result.put(user.getId(), user);
            }
        }
        return result;
    }

    private static UserDTO toUser(Object value) {
        if (value instanceof UserDTO user) {
            return user;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        UserDTO user = new UserDTO();
        user.setId(asInteger(map.get("id")));
        user.setUsername(asString(map.get("username")));
        user.setNickname(asString(map.get("nickname")));
        user.setAvatar(asString(map.get("avatar")));
        user.setRole(asInteger(map.get("role")));
        user.setBio(asString(map.get("bio")));
        return user;
    }

    private static Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.valueOf(text);
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
