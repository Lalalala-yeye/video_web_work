package com.doinb.user.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 骨架桩：组员替换为查 users 表。 */
@RestController
public class InternalUserController {

    @GetMapping(InternalPaths.SEARCH_USERS)
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return CustomResponse.ok("骨架桩，请按契约实现", List.of());
    }

    @GetMapping(InternalPaths.USERS + "/{id}")
    public CustomResponse getOne(@PathVariable("id") Integer id) {
        return CustomResponse.fail(404, "骨架桩：用户服务尚未实现 GET /internal/users/{id}");
    }

    @GetMapping(InternalPaths.USERS)
    public CustomResponse listByIds(@RequestParam("ids") String ids) {
        return CustomResponse.ok("骨架桩，请按契约实现", List.of());
    }
}
