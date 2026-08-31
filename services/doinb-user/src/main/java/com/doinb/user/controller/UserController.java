package com.doinb.user.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.web.GatewayUser;
import com.doinb.user.pojo.dto.UserPublicDTO;
import com.doinb.user.pojo.dto.UserShowcaseDTO;
import com.doinb.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/user/info/get-one")
    public CustomResponse getOneUserInfo(@RequestParam("uid") Integer uid) {
        UserPublicDTO profile = userService.getPublicProfile(uid);
        if (profile == null) {
            return CustomResponse.fail(404, "用户不存在");
        }
        return CustomResponse.ok(profile);
    }

    @GetMapping("/user/profile/showcase")
    public CustomResponse showcase(@RequestParam("uid") Integer uid,
                                   @RequestParam(value = "page", defaultValue = "1") long page,
                                   @RequestParam(value = "size", defaultValue = "12") long size,
                                   HttpServletRequest request) {
        UserShowcaseDTO data = userService.getShowcase(uid, page, size);
        if (data == null) {
            return CustomResponse.fail(404, "用户不存在");
        }
        Integer viewerId = GatewayUser.userId(request);
        if (viewerId == null || viewerId.equals(uid)) {
            data.setFollowing(false);
        }
        return CustomResponse.ok(data);
    }

    @PostMapping("/user/info/update")
    public CustomResponse updateUserInfo(@RequestParam("nickname") String nickname,
                                         @RequestParam(value = "bio", required = false) String bio,
                                         HttpServletRequest request) {
        return userService.updateUserInfo(GatewayUser.requireUserId(request), nickname, bio);
    }

    @PostMapping("/user/avatar/upload")
    public CustomResponse uploadAvatar(@RequestParam("file") MultipartFile file,
                                       HttpServletRequest request) {
        return userService.uploadAvatar(GatewayUser.requireUserId(request), file);
    }
}
