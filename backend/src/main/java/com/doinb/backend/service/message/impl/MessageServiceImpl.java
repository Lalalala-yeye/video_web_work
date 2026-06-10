package com.doinb.backend.service.message.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.DmMessageMapper;
import com.doinb.backend.mapper.DmRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.DmMessageDTO;
import com.doinb.backend.pojo.dto.DmRoomDTO;
import com.doinb.backend.pojo.entity.DmMessage;
import com.doinb.backend.pojo.entity.DmRoom;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.message.MessageService;
import com.doinb.backend.service.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class MessageServiceImpl implements MessageService {

    private final DmRoomMapper dmRoomMapper;
    private final DmMessageMapper dmMessageMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public MessageServiceImpl(DmRoomMapper dmRoomMapper,
                              DmMessageMapper dmMessageMapper,
                              UserMapper userMapper,
                              NotificationService notificationService) {
        this.dmRoomMapper = dmRoomMapper;
        this.dmMessageMapper = dmMessageMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    @Override
    public CustomResponse openRoom(Integer userId, Integer peerId) {
        if (peerId == null) {
            return fail(400, "对方用户 id 不能为空");
        }
        if (Objects.equals(userId, peerId)) {
            return fail(400, "不能给自己发私信");
        }
        if (userMapper.selectById(peerId) == null) {
            return fail(404, "用户不存在");
        }

        DmRoom room = findRoom(userId, peerId);
        if (room == null) {
            room = new DmRoom();
            room.setUserA(Math.min(userId, peerId));
            room.setUserB(Math.max(userId, peerId));
            room.setUpdateTime(LocalDateTime.now());
            dmRoomMapper.insert(room);
        }

        DmRoomDTO dto = buildRoomDTO(room, userId, 1, 50);
        CustomResponse resp = ok("OK");
        resp.setData(dto);
        return resp;
    }

    @Override
    public CustomResponse getRoom(Integer userId, Integer roomId, long page, long size) {
        DmRoom room = dmRoomMapper.selectById(roomId);
        if (room == null || !isMember(room, userId)) {
            return fail(404, "会话不存在");
        }
        CustomResponse resp = ok("OK");
        resp.setData(buildRoomDTO(room, userId, page, size));
        return resp;
    }

    @Override
    public CustomResponse send(Integer userId, Integer roomId, String content) {
        if (!StringUtils.hasText(content)) {
            return fail(400, "消息不能为空");
        }
        if (content.length() > 500) {
            return fail(400, "消息不能超过500字");
        }
        DmRoom room = dmRoomMapper.selectById(roomId);
        if (room == null || !isMember(room, userId)) {
            return fail(404, "会话不存在");
        }

        DmMessage msg = new DmMessage();
        msg.setRoomId(roomId);
        msg.setSenderId(userId);
        msg.setContent(content.trim());
        msg.setCreateTime(LocalDateTime.now());
        dmMessageMapper.insert(msg);

        room.setUpdateTime(LocalDateTime.now());
        dmRoomMapper.updateById(room);

        Integer peerId = peerId(room, userId);
        notificationService.notifyMessage(userId, peerId, roomId, content.trim());

        CustomResponse resp = ok("发送成功");
        resp.setData(toMessageDTO(msg, userMapper.selectById(userId), userId));
        return resp;
    }

    private DmRoom findRoom(Integer userId, Integer peerId) {
        int a = Math.min(userId, peerId);
        int b = Math.max(userId, peerId);
        return dmRoomMapper.selectOne(new LambdaQueryWrapper<DmRoom>()
                .eq(DmRoom::getUserA, a)
                .eq(DmRoom::getUserB, b));
    }

    private boolean isMember(DmRoom room, Integer userId) {
        return Objects.equals(room.getUserA(), userId) || Objects.equals(room.getUserB(), userId);
    }

    private Integer peerId(DmRoom room, Integer userId) {
        return Objects.equals(room.getUserA(), userId) ? room.getUserB() : room.getUserA();
    }

    private DmRoomDTO buildRoomDTO(DmRoom room, Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 20 : Math.min(size, 100);

        Integer peerId = peerId(room, userId);
        User peer = userMapper.selectById(peerId);

        Page<DmMessage> mpPage = new Page<>(safePage, safeSize);
        dmMessageMapper.selectPage(mpPage, new LambdaQueryWrapper<DmMessage>()
                .eq(DmMessage::getRoomId, room.getId())
                .orderByAsc(DmMessage::getCreateTime));

        List<DmMessageDTO> messages = new ArrayList<>();
        for (DmMessage msg : mpPage.getRecords()) {
            User sender = userMapper.selectById(msg.getSenderId());
            messages.add(toMessageDTO(msg, sender, userId));
        }

        DmRoomDTO dto = new DmRoomDTO();
        dto.setRoomId(room.getId());
        dto.setPeerId(peerId);
        dto.setPeerNickname(peer != null ? peer.getNickname() : "用户");
        dto.setPeerAvatar(peer != null ? peer.getAvatar() : null);
        dto.setMessages(messages);
        return dto;
    }

    private DmMessageDTO toMessageDTO(DmMessage msg, User sender, Integer viewerId) {
        DmMessageDTO dto = new DmMessageDTO();
        dto.setId(msg.getId());
        dto.setRoomId(msg.getRoomId());
        dto.setSenderId(msg.getSenderId());
        dto.setSenderNickname(sender != null ? sender.getNickname() : "用户");
        dto.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        dto.setContent(msg.getContent());
        dto.setCreateTime(msg.getCreateTime());
        dto.setMine(Objects.equals(msg.getSenderId(), viewerId));
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
