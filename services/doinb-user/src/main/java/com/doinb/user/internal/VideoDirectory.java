package com.doinb.user.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.VideoDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 用户主页作品列表走视频服务内部接口，不查 videos 表。 */
@Component
public class VideoDirectory {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public VideoDirectory(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    public List<VideoDTO> listPublishedByAuthors(Collection<Integer> authorIds, long limit) {
        if (authorIds == null || authorIds.isEmpty()) {
            return List.of();
        }
        String csv = authorIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (csv.isEmpty()) {
            return List.of();
        }
        CustomResponse resp = serviceClient.get(
                properties.getServices().getVideo(),
                ServiceClient.query(
                        ServiceClient.query(InternalPaths.VIDEOS + "/by-authors", "authorIds", csv),
                        "limit", String.valueOf(limit)));
        return toVideoList(resp.getData());
    }

    private static List<VideoDTO> toVideoList(Object data) {
        if (!(data instanceof Collection<?> rows)) {
            return List.of();
        }
        List<VideoDTO> result = new ArrayList<>();
        for (Object row : rows) {
            VideoDTO video = toVideo(row);
            if (video != null) {
                result.add(video);
            }
        }
        return result;
    }

    private static VideoDTO toVideo(Object value) {
        if (value instanceof VideoDTO video) {
            return video;
        }
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        VideoDTO video = new VideoDTO();
        video.setId(asInteger(map.get("id")));
        video.setTitle(asString(map.get("title")));
        video.setAuthorId(asInteger(map.get("authorId")));
        video.setAuthorNickname(asString(map.get("authorNickname")));
        video.setCoverUrl(asString(map.get("coverUrl")));
        video.setVideoUrl(asString(map.get("videoUrl")));
        video.setStatus(asInteger(map.get("status")));
        return video;
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
