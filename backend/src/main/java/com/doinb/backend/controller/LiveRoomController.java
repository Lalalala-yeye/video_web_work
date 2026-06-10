package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.LiveRoomDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.service.live.LiveRoomService;
import com.doinb.backend.service.users.impl.UserDetailsImpl;
import com.doinb.backend.service.utils.CurrentUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 直播间接口 */
@RestController
public class LiveRoomController {

    private final LiveRoomService liveRoomService;
    private final CurrentUser currentUser;

    public LiveRoomController(LiveRoomService liveRoomService, CurrentUser currentUser) {
        this.liveRoomService = liveRoomService;
        this.currentUser = currentUser;
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
    public CustomResponse getOne(@RequestParam("id") Integer id) {
        return liveRoomService.getOne(id);
    }

    @PostMapping("/live/create")
    public CustomResponse create(@RequestParam("title") String title) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return liveRoomService.create(loginUser.getUser().getId(), loginUser.getUser().getRole(), title);
    }

    @PostMapping("/live/start")
    public CustomResponse start(@RequestParam("id") Integer id) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return liveRoomService.startLive(loginUser.getUser().getId(), loginUser.getUser().getRole(), id);
    }

    @PostMapping("/live/stop")
    public CustomResponse stop(@RequestParam("id") Integer id) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return liveRoomService.stopLive(loginUser.getUser().getId(), loginUser.getUser().getRole(), id);
    }

    @GetMapping("/live/my/list")
    public CustomResponse myList(@RequestParam(value = "page", defaultValue = "1") long page,
                                 @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<LiveRoomDTO> result = liveRoomService.listMyRooms(currentUser.getUserId(), page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }
}
