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
