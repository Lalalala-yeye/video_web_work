package com.doinb.message.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.web.GatewayUser;
import com.doinb.message.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/message/room/open")
    public CustomResponse openRoom(HttpServletRequest request,
                                   @RequestParam("peerId") Integer peerId) {
        return messageService.openRoom(GatewayUser.requireUserId(request), peerId);
    }

    @GetMapping("/message/room/get")
    public CustomResponse getRoom(HttpServletRequest request,
                                  @RequestParam("roomId") Integer roomId,
                                  @RequestParam(value = "page", defaultValue = "1") long page,
                                  @RequestParam(value = "size", defaultValue = "50") long size) {
        return messageService.getRoom(GatewayUser.requireUserId(request), roomId, page, size);
    }

    @PostMapping("/message/send")
    public CustomResponse send(HttpServletRequest request,
                               @RequestParam("roomId") Integer roomId,
                               @RequestParam("content") String content) {
        return messageService.send(GatewayUser.requireUserId(request), roomId, content);
    }
}
