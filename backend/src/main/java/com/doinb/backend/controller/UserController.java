package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.service.users.UserService;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户资料接口：查公开信息、修改昵称和头像。
 */
@RestController
public class UserController {

    private final UserService userService;
    private final CurrentUser currentUser;

    public UserController(UserService userService, CurrentUser currentUser) {
        this.userService = userService;
        this.currentUser = currentUser;
    }

    /** 根据用户 id 查询公开资料（无需登录） */
    @GetMapping("/user/info/get-one")
    public CustomResponse getOneUserInfo(@RequestParam("uid") Integer uid) {
        CustomResponse resp = new CustomResponse();
        resp.setData(userService.getUserById(uid));
        return resp;
    }

    /** 修改当前登录用户的昵称和头像 URL（需登录） */
    @PostMapping("/user/info/update")
    public CustomResponse updateUserInfo(@RequestParam("nickname") String nickname,
                                         @RequestParam(value = "avatar", required = false) String avatar) {
        Integer userId = currentUser.getUserId();
        return userService.updateUserInfo(userId, nickname, avatar);
    }
}
