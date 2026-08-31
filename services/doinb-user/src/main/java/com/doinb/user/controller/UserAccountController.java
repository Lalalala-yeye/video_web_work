package com.doinb.user.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.web.GatewayUser;
import com.doinb.user.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserAccountController {

    private final UserAccountService userAccountService;

    public UserAccountController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/user/account/register")
    public CustomResponse register(@RequestBody Map<String, String> body) {
        return userAccountService.register(
                body.get("username"),
                body.get("password"),
                body.get("confirmedPassword")
        );
    }

    @PostMapping("/user/account/login")
    public CustomResponse login(@RequestBody Map<String, String> body) {
        return userAccountService.login(body.get("username"), body.get("password"));
    }

    @PostMapping("/admin/account/login")
    public CustomResponse adminLogin(@RequestBody Map<String, String> body) {
        return userAccountService.adminLogin(body.get("username"), body.get("password"));
    }

    @GetMapping("/user/personal/info")
    public CustomResponse personalInfo(HttpServletRequest request) {
        return userAccountService.personalInfo(GatewayUser.requireUserId(request));
    }

    @GetMapping("/admin/personal/info")
    public CustomResponse adminPersonalInfo(HttpServletRequest request) {
        return userAccountService.adminPersonalInfo(
                GatewayUser.requireUserId(request),
                GatewayUser.isAdmin(request));
    }

    @GetMapping("/user/account/logout")
    public CustomResponse logout() {
        return userAccountService.logout();
    }

    @PostMapping("/user/password/update")
    public CustomResponse updatePassword(@RequestParam("pw") String oldPassword,
                                         @RequestParam("npw") String newPassword,
                                         HttpServletRequest request) {
        return userAccountService.updatePassword(
                GatewayUser.requireUserId(request), oldPassword, newPassword);
    }
}
