package com.doinb.backend.service.live.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.LiveRoomDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.live.LiveRoomService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LiveRoomServiceImpl implements LiveRoomService {

    private static final String DEMO_PLAY_PREFIX = "http://localhost:8080/live/play/";

    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;

    public LiveRoomServiceImpl(LiveRoomMapper liveRoomMapper, UserMapper userMapper) {
        this.liveRoomMapper = liveRoomMapper;
        this.userMapper = userMapper;
    }

    @Override
    public PageResult<LiveRoomDTO> list(long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<LiveRoom> mpPage = new Page<>(safePage, safeSize);
        liveRoomMapper.selectPage(mpPage, new LambdaQueryWrapper<LiveRoom>()
                .orderByDesc(LiveRoom::getId));

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toDTOList(mpPage.getRecords()));
    }

    @Override
    public CustomResponse getOne(Integer id) {
        LiveRoom room = liveRoomMapper.selectById(id);
        if (room == null) {
            return fail(404, "直播间不存在");
        }
        CustomResponse resp = ok("OK");
        resp.setData(toDTO(room));
        return resp;
    }

    @Override
    public CustomResponse create(Integer userId, Integer role, String title) {
        if (!StringUtils.hasText(title)) {
            return fail(400, "标题不能为空");
        }
        if (title.length() > 100) {
            return fail(400, "标题长度不能超过100");
        }

        LiveRoom room = new LiveRoom();
        room.setTitle(title.trim());
        room.setAnchorId(userId);
        room.setStreamKey(UUID.randomUUID().toString().replace("-", ""));
        room.setIsLive(false);
        liveRoomMapper.insert(room);

        CustomResponse resp = ok("创建成功");
        resp.setData(toDTO(room));
        return resp;
    }

    @Override
    public CustomResponse startLive(Integer userId, Integer role, Integer roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            return fail(404, "直播间不存在");
        }
        if (!canManage(userId, role, room)) {
            return fail(403, "无权开播");
        }
        if (Boolean.TRUE.equals(room.getIsLive())) {
            return fail(400, "已在直播中");
        }

        liveRoomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, roomId)
                .set(LiveRoom::getIsLive, true));
        return ok("开播成功");
    }

    @Override
    public CustomResponse stopLive(Integer userId, Integer role, Integer roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            return fail(404, "直播间不存在");
        }
        if (!canManage(userId, role, room)) {
            return fail(403, "无权停播");
        }
        if (!Boolean.TRUE.equals(room.getIsLive())) {
            return fail(400, "当前未在直播");
        }

        liveRoomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, roomId)
                .set(LiveRoom::getIsLive, false));
        return ok("停播成功");
    }

    @Override
    public PageResult<LiveRoomDTO> listMyRooms(Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<LiveRoom> mpPage = new Page<>(safePage, safeSize);
        liveRoomMapper.selectPage(mpPage, new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getAnchorId, userId)
                .orderByDesc(LiveRoom::getId));

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toDTOList(mpPage.getRecords()));
    }

    private boolean canManage(Integer userId, Integer role, LiveRoom room) {
        if (role != null && role == 2) {
            return true;
        }
        return Objects.equals(room.getAnchorId(), userId);
    }

    private List<LiveRoomDTO> toDTOList(List<LiveRoom> rooms) {
        if (rooms.isEmpty()) {
            return List.of();
        }
        List<Integer> anchorIds = rooms.stream()
                .map(LiveRoom::getAnchorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> anchorMap = userMapper.selectBatchIds(anchorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<LiveRoomDTO> list = new ArrayList<>();
        for (LiveRoom room : rooms) {
            list.add(toDTO(room, anchorMap.get(room.getAnchorId())));
        }
        return list;
    }

    private LiveRoomDTO toDTO(LiveRoom room) {
        User anchor = userMapper.selectById(room.getAnchorId());
        return toDTO(room, anchor);
    }

    private LiveRoomDTO toDTO(LiveRoom room, User anchor) {
        LiveRoomDTO dto = new LiveRoomDTO();
        dto.setId(room.getId());
        dto.setTitle(room.getTitle());
        dto.setAnchorId(room.getAnchorId());
        dto.setAnchorNickname(anchor != null ? anchor.getNickname() : "未知主播");
        dto.setStreamKey(room.getStreamKey());
        dto.setIsLive(room.getIsLive());
        dto.setPlayUrl(DEMO_PLAY_PREFIX + room.getStreamKey());
        return dto;
    }

    private CustomResponse ok(String message) {
        CustomResponse resp = new CustomResponse();
        resp.setMessage(message);
        return resp;
    }

    private CustomResponse fail(int code, String message) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}
