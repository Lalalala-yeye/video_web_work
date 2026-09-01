package com.doinb.interact.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.web.GatewayUser;
import com.doinb.interact.pojo.dto.FeedItemDTO;
import com.doinb.interact.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 订阅/关注接口（UC-09） */
@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/subscription/follow")
    public CustomResponse follow(HttpServletRequest request,
                                 @RequestParam("targetId") Integer targetId) {
        return subscriptionService.follow(GatewayUser.requireUserId(request), targetId);
    }

    @PostMapping("/subscription/unfollow")
    public CustomResponse unfollow(HttpServletRequest request,
                                   @RequestParam("targetId") Integer targetId) {
        return subscriptionService.unfollow(GatewayUser.requireUserId(request), targetId);
    }

    /** 查询是否已关注某用户（需登录） */
    @GetMapping("/subscription/status")
    public CustomResponse status(HttpServletRequest request,
                                 @RequestParam("targetId") Integer targetId) {
        boolean following = subscriptionService.isFollowing(GatewayUser.requireUserId(request), targetId);
        Map<String, Object> data = new HashMap<>();
        data.put("following", following);
        return CustomResponse.ok(data);
    }

    @GetMapping("/subscription/following")
    public CustomResponse following(HttpServletRequest request,
                                    @RequestParam(value = "page", defaultValue = "1") long page,
                                    @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<UserDTO> result = subscriptionService.listFollowing(
                GatewayUser.requireUserId(request), page, size);
        return CustomResponse.ok(result);
    }

    @GetMapping("/subscription/feed")
    public CustomResponse feed(HttpServletRequest request,
                               @RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<FeedItemDTO> result = subscriptionService.feed(
                GatewayUser.requireUserId(request), page, size);
        return CustomResponse.ok(result);
    }
}
