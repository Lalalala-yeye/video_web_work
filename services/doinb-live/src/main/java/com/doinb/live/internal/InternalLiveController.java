package com.doinb.live.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.live.config.LiveStreamHelper;
import com.doinb.live.pojo.LiveRoom;
import com.doinb.live.service.impl.LiveRoomServiceImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 内部接口：供互动（弹幕/订阅动态）和网关（搜索）调用，不暴露给浏览器 */
@RestController
public class InternalLiveController {

    private final LiveRoomServiceImpl liveRoomService;
    private final LiveStreamHelper liveStreamHelper;

    public InternalLiveController(LiveRoomServiceImpl liveRoomService, LiveStreamHelper liveStreamHelper) {
        this.liveRoomService = liveRoomService;
        this.liveStreamHelper = liveStreamHelper;
    }

    /** 互动发弹幕前查房间：必须带 isLive、sessionStart */
    @GetMapping(InternalPaths.LIVES + "/{id}")
    public CustomResponse getOne(@PathVariable("id") Integer id) {
        LiveRoom room = liveRoomService.getRoomById(id);
        if (room == null) {
            return CustomResponse.fail(404, "直播间不存在");
        }
        LiveRoomDTO dto = new LiveRoomDTO();
        dto.setId(room.getId());
        dto.setTitle(room.getTitle());
        dto.setAnchorId(room.getAnchorId());
        dto.setIsLive(room.getIsLive());
        dto.setSessionStart(room.getSessionStart());
        return CustomResponse.ok("OK", dto);
    }

    /** 互动订阅动态：按主播 ID 列表查开播中的房间 */
    @GetMapping(InternalPaths.LIVES + "/by-anchors")
    public CustomResponse byAnchors(@RequestParam("anchorIds") String anchorIds,
                                    @RequestParam(value = "limit", defaultValue = "50") long limit) {
        List<Integer> ids = parseIds(anchorIds);
        List<LiveRoom> rooms = liveRoomService.findLiveByAnchors(ids, limit);
        List<LiveRoomDTO> dtos = new ArrayList<>();
        for (LiveRoom room : rooms) {
            LiveRoomDTO dto = new LiveRoomDTO();
            dto.setId(room.getId());
            dto.setTitle(room.getTitle());
            dto.setAnchorId(room.getAnchorId());
            dto.setIsLive(room.getIsLive());
            dto.setSessionStart(room.getSessionStart());
            dtos.add(dto);
        }
        return CustomResponse.ok("OK", dtos);
    }

    /** 网关搜索：只返回开播中的房间，并补齐主播昵称 / playUrl */
    @GetMapping(InternalPaths.SEARCH_LIVES)
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "limit", defaultValue = "10") long limit) {
        return CustomResponse.ok(liveRoomService.searchPublished(keyword, limit));
    }

    private List<Integer> parseIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(raw.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Integer::valueOf)
                    .toList();
        } catch (NumberFormatException e) {
            return List.of();
        }
    }
}
