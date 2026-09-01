package com.doinb.interact.client;

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
import java.util.Objects;
import java.util.stream.Collectors;

/** 只通过用户服务内部接口取用户信息，不直接访问 users 表。 */
@Component
public class UserDirectory {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public UserDirectory(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    /** 目标用户是否存在（关注前校验）。不存在返回 null。 */
    public UserDTO findById(Integer id) {
        if (id == null) {
            return null;
        }
        CustomResponse resp = serviceClient.get(
                properties.getServices().getUser(),
                InternalPaths.USERS + "/" + id);
        if (resp.getCode() != 200 || resp.getData() == null) {
            return null;
        }
        return toUser(resp.getData());
    }

    /** 批量取用户（拼昵称）。缺的 id 跳过。 */
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
        CustomResponse resp = serviceClient.get(
                properties.getServices().getUser(),
                ServiceClient.query(InternalPaths.USERS, "ids", csv));
        if (resp.getCode() != 200 || !(resp.getData() instanceof Collection<?> rows)) {
            return Map.of();
        }
        Map<Integer, UserDTO> result = new HashMap<>();
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
