package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserPublicDTO;
import com.doinb.backend.pojo.dto.UserShowcaseDTO;
import com.doinb.backend.service.subscription.SubscriptionService;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.users.impl.UserDetailsImpl;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 用户资料接口 */
@RestController
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;
    private final SubscriptionService subscriptionService;

    public UserController(UserService userService,
                          CurrentUser currentUser,
                          SubscriptionService subscriptionService) {
        this.userService = userService;
        this.currentUser = currentUser;
        this.subscriptionService = subscriptionService;
    }

    /** 对外公开资料（不含账号等隐私） */
    @GetMapping("/user/info/get-one")
    public CustomResponse getOneUserInfo(@RequestParam("uid") Integer uid) {
        UserPublicDTO profile = userService.getPublicProfile(uid);
        if (profile == null) {
            CustomResponse resp = new CustomResponse();
            resp.setCode(404);
            resp.setMessage("用户不存在");
            return resp;
        }
        CustomResponse resp = new CustomResponse();
        resp.setData(profile);
        return resp;
    }

    /** 用户公开展示页：简介 + 已发布作品 */
    @GetMapping("/user/profile/showcase")
    public CustomResponse showcase(@RequestParam("uid") Integer uid,
                                   @RequestParam(value = "page", defaultValue = "1") long page,
                                   @RequestParam(value = "size", defaultValue = "12") long size) {
        UserShowcaseDTO data = userService.getShowcase(uid, page, size);
        if (data == null) {
            CustomResponse resp = new CustomResponse();
            resp.setCode(404);
            resp.setMessage("用户不存在");
            return resp;
        }
        CustomResponse resp = new CustomResponse();
        Integer viewerId = resolveViewerId();
        if (viewerId != null && !viewerId.equals(uid)) {
            data.setFollowing(subscriptionService.isFollowing(viewerId, uid));
        } else {
            data.setFollowing(false);
        }
        resp.setData(data);
        return resp;
    }

    private Integer resolveViewerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetailsImpl loginUser && loginUser.getUser() != null) {
            return loginUser.getUser().getId();
        }
        return null;
    }

    /** 修改昵称与个人简介 */
    @PostMapping("/user/info/update")
    public CustomResponse updateUserInfo(@RequestParam("nickname") String nickname,
                                         @RequestParam(value = "bio", required = false) String bio) {
        Integer userId = currentUser.getUserId();
        return userService.updateUserInfo(userId, nickname, bio);
    }

    /** 上传头像图片 */
    @PostMapping("/user/avatar/upload")
    public CustomResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        return userService.uploadAvatar(currentUser.getUserId(), file);
    }
}
