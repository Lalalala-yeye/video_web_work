package com.doinb.interact.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.web.GatewayUser;
import com.doinb.interact.pojo.dto.CommentDTO;
import com.doinb.interact.service.CommentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 评论接口（UC-10）：视频评论与直播弹幕 */
@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/comment/add")
    public CustomResponse add(HttpServletRequest request,
                              @RequestParam("targetId") Integer targetId,
                              @RequestParam("targetType") Integer targetType,
                              @RequestParam("content") String content) {
        return commentService.add(GatewayUser.requireUserId(request), targetId, targetType, content);
    }

    /** 公开：未登录也可查看评论列表 */
    @GetMapping("/comment/list")
    public CustomResponse list(HttpServletRequest request,
                               @RequestParam("targetId") Integer targetId,
                               @RequestParam("targetType") Integer targetType,
                               @RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "20") long size) {
        PageResult<CommentDTO> result = commentService.listByTarget(
                targetId, targetType, page, size, GatewayUser.userId(request));
        return CustomResponse.ok(result);
    }
}
