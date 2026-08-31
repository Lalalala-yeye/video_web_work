package com.doinb.video.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 骨架桩：组员替换为查 videos 表。 */
@RestController
public class InternalVideoController {

    @GetMapping(InternalPaths.SEARCH_VIDEOS)
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return CustomResponse.ok("骨架桩，请按契约实现", List.of());
    }

    @GetMapping(InternalPaths.VIDEOS + "/{id}")
    public CustomResponse getOne(@PathVariable("id") Integer id) {
        return CustomResponse.fail(404, "骨架桩：视频服务尚未实现 GET /internal/videos/{id}");
    }

    @GetMapping(InternalPaths.VIDEOS + "/by-authors")
    public CustomResponse byAuthors(@RequestParam("authorIds") String authorIds,
                                    @RequestParam(value = "limit", defaultValue = "100") long limit) {
        return CustomResponse.ok("骨架桩，请按契约实现", List.of());
    }
}
