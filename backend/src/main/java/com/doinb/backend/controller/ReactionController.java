package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.service.reaction.ReactionService;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 点赞/点踩 */
@RestController
public class ReactionController {

    private final ReactionService reactionService;
    private final CurrentUser currentUser;

    public ReactionController(ReactionService reactionService, CurrentUser currentUser) {
        this.reactionService = reactionService;
        this.currentUser = currentUser;
    }

    @GetMapping("/video/reaction/summary")
    public CustomResponse videoSummary(@RequestParam("videoId") Integer videoId) {
        Integer viewerId = safeViewerId();
        CustomResponse resp = new CustomResponse();
        resp.setData(reactionService.getVideoSummary(videoId, viewerId));
        return resp;
    }

    @PostMapping("/video/reaction")
    public CustomResponse videoReaction(@RequestParam("videoId") Integer videoId,
                                        @RequestParam("reaction") Integer reaction) {
        return reactionService.setVideoReaction(currentUser.getUserId(), videoId, reaction);
    }

    @PostMapping("/comment/reaction")
    public CustomResponse commentReaction(@RequestParam("commentId") Integer commentId,
                                          @RequestParam("reaction") Integer reaction) {
        return reactionService.setCommentReaction(currentUser.getUserId(), commentId, reaction);
    }

    private Integer safeViewerId() {
        try {
            return currentUser.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
