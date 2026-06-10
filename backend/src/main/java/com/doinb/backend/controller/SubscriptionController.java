package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.FeedItemDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.service.subscription.SubscriptionService;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/** 订阅/关注接口 */
@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CurrentUser currentUser;

    public SubscriptionController(SubscriptionService subscriptionService, CurrentUser currentUser) {
        this.subscriptionService = subscriptionService;
        this.currentUser = currentUser;
    }

    @PostMapping("/subscription/follow")
    public CustomResponse follow(@RequestParam("targetId") Integer targetId) {
        return subscriptionService.follow(currentUser.getUserId(), targetId);
    }

    @PostMapping("/subscription/unfollow")
    public CustomResponse unfollow(@RequestParam("targetId") Integer targetId) {
        return subscriptionService.unfollow(currentUser.getUserId(), targetId);
    }

    /** 查询是否已关注某用户（需登录） */
    @GetMapping("/subscription/status")
    public CustomResponse status(@RequestParam("targetId") Integer targetId) {
        boolean following = subscriptionService.isFollowing(currentUser.getUserId(), targetId);
        Map<String, Object> data = new HashMap<>();
        data.put("following", following);
        CustomResponse resp = new CustomResponse();
        resp.setData(data);
        return resp;
    }

    @GetMapping("/subscription/following")
    public CustomResponse following(@RequestParam(value = "page", defaultValue = "1") long page,
                                      @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<UserDTO> result = subscriptionService.listFollowing(currentUser.getUserId(), page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    @GetMapping("/subscription/feed")
    public CustomResponse feed(@RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<FeedItemDTO> result = subscriptionService.feed(currentUser.getUserId(), page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }
}
