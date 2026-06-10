package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.service.users.UserAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 账号相关接口：注册、登录、登出、改密码、获取当前用户信息。
 */
@RestController
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    /** 用户注册 */
    @PostMapping("/user/account/register")
    public CustomResponse register(@RequestBody Map<String, String> body) {
        return userAccountService.register(
                body.get("username"),
                body.get("password"),
                body.get("confirmedPassword")
        );
    }

    /** 普通用户登录 */
    @PostMapping("/user/account/login")
    public CustomResponse login(@RequestBody Map<String, String> body) {
        return userAccountService.login(body.get("username"), body.get("password"));
    }

    /** 管理员登录 */
    @PostMapping("/admin/account/login")
    public CustomResponse adminLogin(@RequestBody Map<String, String> body) {
        return userAccountService.adminLogin(body.get("username"), body.get("password"));
    }

    /** 获取当前登录用户信息（需带 token） */
    @GetMapping("/user/personal/info")
    public CustomResponse personalInfo() {
        return userAccountService.personalInfo();
    }

    /** 获取当前登录管理员信息（需带 token 且 role=2） */
    @GetMapping("/admin/personal/info")
    public CustomResponse adminPersonalInfo() {
        return userAccountService.adminPersonalInfo();
    }

    /** 退出登录 */
    @GetMapping("/user/account/logout")
    public CustomResponse logout() {
        return userAccountService.logout();
    }

    /** 修改密码 */
    @PostMapping("/user/password/update")
    public CustomResponse updatePassword(@RequestParam("pw") String oldPassword,
                                         @RequestParam("npw") String newPassword) {
        return userAccountService.updatePassword(oldPassword, newPassword);
    }
}
