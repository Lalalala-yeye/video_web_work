package com.doinb.interact.client;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.LiveRoomDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 只通过直播服务内部接口取直播间信息，不直接访问 live_rooms 表。 */
@Component
public class LiveDirectory {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public LiveDirectory(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    /** 发弹幕前查房间；不存在返回 null。 */
    public LiveRoomDTO findById(Integer id) {
        if (id == null) {
            return null;
        }
        CustomResponse resp = serviceClient.get(
                properties.getServices().getLive(),
                InternalPaths.LIVES + "/" + id);
        if (resp.getCode() != 200 || resp.getData() == null) {
            return null;
        }
        return toRoom(resp.getData());
    }

    /** 订阅动态：关注主播正在直播的房间。 */
    public List<LiveRoomDTO> listLiveByAnchors(Collection<Integer> anchorIds, long limit) {
        if (anchorIds == null || anchorIds.isEmpty()) {
            return List.of();
        }
        String csv = anchorIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (csv.isEmpty()) {
            return List.of();
        }
        CustomResponse resp = serviceClient.get(
                properties.getServices().getLive(),
                ServiceClient.query(
                        ServiceClient.query(InternalPaths.LIVES + "/by-anchors", "anchorIds", csv),
                        "limit", String.valueOf(limit)));
        return toRoomList(resp.getData());
    }

    private static List<LiveRoomDTO> toRoomList(Object data) {
        if (!(data instanceof Collection<?> rows)) {
            return List.of();
        }
        List<LiveRoomDTO> result = new ArrayList<>();
        for (Object row : rows) {
            LiveRoomDTO room = toRoom(row);
            if (room != null) {
                result.add(room);
            }
        }
        return result;
    }

    private static LiveRoomDTO toRoom(Object value) {
        if (value instanceof LiveRoomDTO room) {
            return room;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        LiveRoomDTO room = new LiveRoomDTO();
        room.setId(asInteger(map.get("id")));
        room.setTitle(asString(map.get("title")));
        room.setAnchorId(asInteger(map.get("anchorId")));
        room.setAnchorNickname(asString(map.get("anchorNickname")));
        room.setStreamKey(asString(map.get("streamKey")));
        room.setIsLive(asBoolean(map.get("isLive")));
        room.setPlayUrl(asString(map.get("playUrl")));
        return room;
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

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.valueOf(text);
        }
        return null;
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
