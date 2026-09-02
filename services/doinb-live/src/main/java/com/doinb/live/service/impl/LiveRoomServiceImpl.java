package com.doinb.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.common.dto.UserDTO;
import com.doinb.live.config.LiveStreamHelper;
import com.doinb.live.mapper.LiveRoomMapper;
import com.doinb.live.pojo.LiveRoom;
import com.doinb.live.service.LiveRoomService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LiveRoomServiceImpl implements LiveRoomService {

    private final LiveRoomMapper liveRoomMapper;
    private final LiveStreamHelper liveStreamHelper;
    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public LiveRoomServiceImpl(LiveRoomMapper liveRoomMapper,
                               LiveStreamHelper liveStreamHelper,
                               ServiceClient serviceClient,
                               DoinbProperties properties) {
        this.liveRoomMapper = liveRoomMapper;
        this.liveStreamHelper = liveStreamHelper;
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    @Override
    public PageResult<LiveRoomDTO> list(long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<LiveRoom> mpPage = new Page<>(safePage, safeSize);
        liveRoomMapper.selectPage(mpPage, new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, true)
                .orderByDesc(LiveRoom::getSessionStart));

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toDTOList(mpPage.getRecords(), false));
    }

    @Override
    public CustomResponse getOne(Integer id, Integer viewerUserId, boolean viewerIsAdmin) {
        LiveRoom room = liveRoomMapper.selectById(id);
        if (room == null) {
            return fail(404, "直播间不存在");
        }
        if (!Boolean.TRUE.equals(room.getIsLive()) && !canManage(viewerUserId, viewerIsAdmin, room)) {
            return fail(404, "直播间未开播或已结束");
        }
        boolean includePrivate = canManage(viewerUserId, viewerIsAdmin, room);
        CustomResponse resp = ok("OK");
        resp.setData(toDTO(room, includePrivate));
        return resp;
    }

    @Override
    public CustomResponse create(Integer userId, String title) {
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
        room.setCreateTime(LocalDateTime.now());
        liveRoomMapper.insert(room);

        CustomResponse resp = ok("创建成功，请点击开播");
        resp.setData(toDTO(room, true));
        return resp;
    }

    @Override
    public CustomResponse startLive(Integer userId, boolean isAdmin, Integer roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            return fail(404, "直播间不存在");
        }
        if (!canManage(userId, isAdmin, room)) {
            return fail(403, "无权开播");
        }
        if (Boolean.TRUE.equals(room.getIsLive())) {
            return fail(400, "已在直播中");
        }

        LocalDateTime now = LocalDateTime.now();
        liveRoomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, roomId)
                .set(LiveRoom::getIsLive, true)
                .set(LiveRoom::getSessionStart, now)
                .set(LiveRoom::getEndedAt, null));
        return ok("开播成功");
    }

    @Override
    public CustomResponse stopLive(Integer userId, boolean isAdmin, Integer roomId) {
        LiveRoom room = liveRoomMapper.selectById(roomId);
        if (room == null) {
            return fail(404, "直播间不存在");
        }
        if (!canManage(userId, isAdmin, room)) {
            return fail(403, "无权停播");
        }
        if (!Boolean.TRUE.equals(room.getIsLive())) {
            return fail(400, "当前未在直播");
        }

        liveRoomMapper.update(null, new LambdaUpdateWrapper<LiveRoom>()
                .eq(LiveRoom::getId, roomId)
                .set(LiveRoom::getIsLive, false)
                .set(LiveRoom::getEndedAt, LocalDateTime.now()));
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

        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, toDTOList(mpPage.getRecords(), true));
    }

    private boolean canManage(Integer userId, boolean isAdmin, LiveRoom room) {
        if (isAdmin) {
            return true;
        }
        return userId != null && Objects.equals(room.getAnchorId(), userId);
    }

    // ================= 内部接口辅助（供 InternalLiveController 使用） =================

    /** 供 InternalLiveController 使用：按 ID 查房间 */
    public LiveRoom getRoomById(Integer id) {
        return liveRoomMapper.selectById(id);
    }

    /** 供 InternalLiveController 使用：按主播 ID 列表查开播中的房间 */
    public List<LiveRoom> findLiveByAnchors(List<Integer> anchorIds, long limit) {
        if (anchorIds == null || anchorIds.isEmpty()) {
            return List.of();
        }
        return liveRoomMapper.selectList(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, true)
                .in(LiveRoom::getAnchorId, anchorIds)
                .orderByDesc(LiveRoom::getSessionStart)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 100)));
    }

    /** 供 InternalLiveController 使用：关键词模糊搜索直播标题 */
    public List<LiveRoom> searchByKeyword(String keyword, long limit) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        return liveRoomMapper.selectList(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, true)
                .like(LiveRoom::getTitle, keyword.trim())
                .orderByDesc(LiveRoom::getSessionStart)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 50)));
    }

    /** 网关搜索用：补齐主播昵称和播放地址，只返回开播中的房间。 */
    public List<LiveRoomDTO> searchPublished(String keyword, long limit) {
        return toDTOList(searchByKeyword(keyword, limit), false);
    }

    // ================= DTO 转换 =================

    private List<LiveRoomDTO> toDTOList(List<LiveRoom> rooms, boolean includePrivate) {
        if (rooms.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> nicknameMap = fetchNicknames(
                rooms.stream().map(LiveRoom::getAnchorId).distinct().collect(Collectors.toList()));
        List<LiveRoomDTO> list = new ArrayList<>();
        for (LiveRoom room : rooms) {
            list.add(toDTO(room, nicknameMap.getOrDefault(room.getAnchorId(), "未知主播"), includePrivate));
        }
        return list;
    }

    private LiveRoomDTO toDTO(LiveRoom room, boolean includePrivate) {
        String nickname = fetchNicknames(List.of(room.getAnchorId()))
                .getOrDefault(room.getAnchorId(), "未知主播");
        return toDTO(room, nickname, includePrivate);
    }

    private LiveRoomDTO toDTO(LiveRoom room, String anchorNickname, boolean includePrivate) {
        LiveRoomDTO dto = new LiveRoomDTO();
        dto.setId(room.getId());
        dto.setTitle(room.getTitle());
        dto.setAnchorId(room.getAnchorId());
        dto.setAnchorNickname(anchorNickname);
        if (includePrivate) {
            dto.setStreamKey(room.getStreamKey());
        }
        dto.setIsLive(room.getIsLive());
        dto.setSessionStart(room.getSessionStart());
        if (Boolean.TRUE.equals(room.getIsLive()) && room.getStreamKey() != null) {
            dto.setPlayUrl(liveStreamHelper.playUrl(room.getStreamKey()));
        }
        return dto;
    }

    /** 调用户服务 /internal/users?ids= 拿昵称；失败降级返回"未知主播" */
    private Map<Integer, String> fetchNicknames(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        try {
            String ids = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            String baseUrl = properties.getServices().getUser();
            CustomResponse resp = serviceClient.get(baseUrl, "/internal/users?ids=" + ids);
            if (resp.getCode() == 200 && resp.getData() instanceof List<?> list) {
                Map<Integer, String> map = new HashMap<>();
                for (Object item : list) {
                    UserDTO u = toUser(item);
                    if (u != null && u.getId() != null) {
                        map.put(u.getId(), u.getNickname() != null ? u.getNickname() : u.getUsername());
                    }
                }
                return map;
            }
        } catch (Exception ignored) {
            // 用户服务不可用时降级
        }
        return Map.of();
    }

    private static UserDTO toUser(Object item) {
        if (item instanceof UserDTO dto) {
            return dto;
        }
        if (!(item instanceof Map<?, ?> raw)) {
            return null;
        }
        UserDTO dto = new UserDTO();
        Object id = raw.get("id");
        if (id instanceof Number n) {
            dto.setId(n.intValue());
        }
        Object nickname = raw.get("nickname");
        dto.setNickname(nickname == null ? null : String.valueOf(nickname));
        Object username = raw.get("username");
        dto.setUsername(username == null ? null : String.valueOf(username));
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
