package com.doinb.video.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.dto.VideoDTO;
import com.doinb.video.service.VideoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class InternalVideoController {

    private final VideoService videoService;

    public InternalVideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping(InternalPaths.SEARCH_VIDEOS)
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return CustomResponse.ok(videoService.searchPublished(keyword, limit));
    }

    @GetMapping(InternalPaths.VIDEOS + "/{id}")
    public CustomResponse getOne(@PathVariable("id") Integer id) {
        VideoDTO video = videoService.getInternal(id);
        if (video == null) {
            return CustomResponse.fail(404, "视频不存在");
        }
        return CustomResponse.ok(video);
    }

    @GetMapping(InternalPaths.VIDEOS + "/by-authors")
    public CustomResponse byAuthors(@RequestParam("authorIds") String authorIds,
                                    @RequestParam(value = "limit", defaultValue = "100") long limit) {
        return CustomResponse.ok(videoService.listPublishedByAuthors(parseIds(authorIds), limit));
    }

    private static List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Integer.valueOf(trimmed));
            }
        }
        return ids;
    }
}
