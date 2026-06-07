package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.CommentDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.service.comment.CommentService;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 评论接口 */
@RestController
public class CommentController {

    private final CommentService commentService;
    private final CurrentUser currentUser;

    public CommentController(CommentService commentService, CurrentUser currentUser) {
        this.commentService = commentService;
        this.currentUser = currentUser;
    }

    @PostMapping("/comment/add")
    public CustomResponse add(@RequestParam("targetId") Integer targetId,
                              @RequestParam("targetType") Integer targetType,
                              @RequestParam("content") String content) {
        return commentService.add(currentUser.getUserId(), targetId, targetType, content);
    }

    @GetMapping("/comment/list")
    public CustomResponse list(@RequestParam("targetId") Integer targetId,
                               @RequestParam("targetType") Integer targetType,
                               @RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "20") long size) {
        PageResult<CommentDTO> result = commentService.listByTarget(targetId, targetType, page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }
}
