package com.doinb.interact.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.web.GatewayUser;
import com.doinb.interact.service.ReactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 点赞/点踩接口（UC-11） */
@RestController
public class ReactionController {

    private final ReactionService reactionService;

    public ReactionController(ReactionService reactionService) {
        this.reactionService = reactionService;
    }

    /** 公开：未登录也可查看赞踩汇总 */
    @GetMapping("/video/reaction/summary")
    public CustomResponse videoSummary(HttpServletRequest request,
                                       @RequestParam("videoId") Integer videoId) {
        return CustomResponse.ok(reactionService.getVideoSummary(videoId, GatewayUser.userId(request)));
    }

    @PostMapping("/video/reaction")
    public CustomResponse videoReaction(HttpServletRequest request,
                                        @RequestParam("videoId") Integer videoId,
                                        @RequestParam("reaction") Integer reaction) {
        return reactionService.setVideoReaction(GatewayUser.requireUserId(request), videoId, reaction);
    }

    @PostMapping("/comment/reaction")
    public CustomResponse commentReaction(HttpServletRequest request,
                                          @RequestParam("commentId") Integer commentId,
                                          @RequestParam("reaction") Integer reaction) {
        return reactionService.setCommentReaction(GatewayUser.requireUserId(request), commentId, reaction);
    }
}
