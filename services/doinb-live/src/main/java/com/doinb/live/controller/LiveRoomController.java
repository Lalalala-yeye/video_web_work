package com.doinb.live.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.common.web.GatewayUser;
import com.doinb.live.service.LiveRoomService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 直播间接口（网关注入 X-User-Id / X-User-Role，本地不再验 JWT） */
@RestController
public class LiveRoomController {

    private final LiveRoomService liveRoomService;

    public LiveRoomController(LiveRoomService liveRoomService) {
        this.liveRoomService = liveRoomService;
    }

    @GetMapping("/live/list")
    public CustomResponse list(@RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<LiveRoomDTO> result = liveRoomService.list(page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    @GetMapping("/live/getone")
    public CustomResponse getOne(@RequestParam("id") Integer id, HttpServletRequest request) {
        Integer viewerId = GatewayUser.userId(request);
        boolean isAdmin = GatewayUser.isAdmin(request);
        return liveRoomService.getOne(id, viewerId, isAdmin);
    }

    @PostMapping("/live/create")
    public CustomResponse create(@RequestParam("title") String title, HttpServletRequest request) {
        Integer userId = GatewayUser.requireUserId(request);
        return liveRoomService.create(userId, title);
    }

    @PostMapping("/live/start")
    public CustomResponse start(@RequestParam("id") Integer id, HttpServletRequest request) {
        Integer userId = GatewayUser.requireUserId(request);
        boolean isAdmin = GatewayUser.isAdmin(request);
        return liveRoomService.startLive(userId, isAdmin, id);
    }

    @PostMapping("/live/stop")
    public CustomResponse stop(@RequestParam("id") Integer id, HttpServletRequest request) {
        Integer userId = GatewayUser.requireUserId(request);
        boolean isAdmin = GatewayUser.isAdmin(request);
        return liveRoomService.stopLive(userId, isAdmin, id);
    }

    @GetMapping("/live/my/list")
    public CustomResponse myList(@RequestParam(value = "page", defaultValue = "1") long page,
                                 @RequestParam(value = "size", defaultValue = "12") long size,
                                 HttpServletRequest request) {
        Integer userId = GatewayUser.requireUserId(request);
        PageResult<LiveRoomDTO> result = liveRoomService.listMyRooms(userId, page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }
}
