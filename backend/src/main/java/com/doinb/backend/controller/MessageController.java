package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.service.message.MessageService;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final MessageService messageService;
    private final CurrentUser currentUser;

    public MessageController(MessageService messageService, CurrentUser currentUser) {
        this.messageService = messageService;
        this.currentUser = currentUser;
    }

    /** 打开与某用户的私信会话（不存在则创建） */
    @PostMapping("/message/room/open")
    public CustomResponse openRoom(@RequestParam("peerId") Integer peerId) {
        return messageService.openRoom(currentUser.getUserId(), peerId);
    }

    @GetMapping("/message/room/get")
    public CustomResponse getRoom(@RequestParam("roomId") Integer roomId,
                                  @RequestParam(value = "page", defaultValue = "1") long page,
                                  @RequestParam(value = "size", defaultValue = "50") long size) {
        return messageService.getRoom(currentUser.getUserId(), roomId, page, size);
    }

    @PostMapping("/message/send")
    public CustomResponse send(@RequestParam("roomId") Integer roomId,
                               @RequestParam("content") String content) {
        return messageService.send(currentUser.getUserId(), roomId, content);
    }
}
