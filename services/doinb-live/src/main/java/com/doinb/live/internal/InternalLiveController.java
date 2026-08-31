package com.doinb.live.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 骨架桩：组员替换为查 live_rooms 表。 */
@RestController
public class InternalLiveController {

    @GetMapping(InternalPaths.SEARCH_LIVES)
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return CustomResponse.ok("骨架桩，请按契约实现", List.of());
    }

    @GetMapping(InternalPaths.LIVES + "/{id}")
    public CustomResponse getOne(@PathVariable("id") Integer id) {
        return CustomResponse.fail(404, "骨架桩：直播服务尚未实现 GET /internal/lives/{id}");
    }

    @GetMapping(InternalPaths.LIVES + "/by-anchors")
    public CustomResponse byAnchors(@RequestParam("anchorIds") String anchorIds,
                                    @RequestParam(value = "limit", defaultValue = "50") long limit) {
        return CustomResponse.ok("骨架桩，请按契约实现", List.of());
    }
}
