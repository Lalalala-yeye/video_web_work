package com.doinb.user.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.dto.UserDTO;
import com.doinb.user.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping(InternalPaths.SEARCH_USERS)
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return CustomResponse.ok(userService.search(keyword, limit));
    }

    @GetMapping(InternalPaths.USERS + "/{id}")
    public CustomResponse getOne(@PathVariable("id") Integer id) {
        UserDTO user = userService.getUserById(id);
        if (user == null) {
            return CustomResponse.fail(404, "用户不存在");
        }
        return CustomResponse.ok(user);
    }

    @GetMapping(InternalPaths.USERS)
    public CustomResponse listByIds(@RequestParam("ids") String ids) {
        return CustomResponse.ok(userService.listByIds(parseIds(ids)));
    }

    private static List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Integer> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            ids.add(Integer.valueOf(trimmed));
        }
        return ids;
    }
}
