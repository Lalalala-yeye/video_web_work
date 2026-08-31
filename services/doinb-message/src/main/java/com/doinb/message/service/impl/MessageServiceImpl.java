package com.doinb.message.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.message.client.UserDirectoryClient;
import com.doinb.message.mapper.DmMessageMapper;
import com.doinb.message.mapper.DmRoomMapper;
import com.doinb.message.pojo.dto.DmMessageDTO;
import com.doinb.message.pojo.dto.DmRoomDTO;
import com.doinb.message.pojo.entity.DmMessage;
import com.doinb.message.pojo.entity.DmRoom;
import com.doinb.message.service.MessageService;
import com.doinb.message.service.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class MessageServiceImpl implements MessageService {

    private final DmRoomMapper dmRoomMapper;
    private final DmMessageMapper dmMessageMapper;
    private final UserDirectoryClient userDirectoryClient;
    private final NotificationService notificationService;

    public MessageServiceImpl(DmRoomMapper dmRoomMapper,
                              DmMessageMapper dmMessageMapper,
                              UserDirectoryClient userDirectoryClient,
                              NotificationService notificationService) {
        this.dmRoomMapper = dmRoomMapper;
        this.dmMessageMapper = dmMessageMapper;
        this.userDirectoryClient = userDirectoryClient;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public CustomResponse openRoom(Integer userId, Integer peerId) {
        if (peerId == null) {
            return CustomResponse.fail(400, "对方用户 id 不能为空");
        }
        if (Objects.equals(userId, peerId)) {
            return CustomResponse.fail(400, "不能给自己发私信");
        }
        if (userDirectoryClient.findById(peerId) == null) {
            return CustomResponse.fail(404, "用户不存在");
        }

        DmRoom room = findRoom(userId, peerId);
        if (room == null) {
            room = new DmRoom();
            room.setUserA(Math.min(userId, peerId));
            room.setUserB(Math.max(userId, peerId));
            room.setUpdateTime(LocalDateTime.now());
            dmRoomMapper.insert(room);
        }
        return CustomResponse.ok("OK", buildRoomDTO(room, userId, 1, 50));
    }

    @Override
    public CustomResponse getRoom(Integer userId, Integer roomId, long page, long size) {
        DmRoom room = dmRoomMapper.selectById(roomId);
        if (room == null || !isMember(room, userId)) {
            return CustomResponse.fail(404, "会话不存在");
        }
        return CustomResponse.ok("OK", buildRoomDTO(room, userId, page, size));
    }

    @Override
    @Transactional
    public CustomResponse send(Integer userId, Integer roomId, String content) {
        if (!StringUtils.hasText(content)) {
            return CustomResponse.fail(400, "消息不能为空");
        }
        String normalizedContent = content.trim();
        if (normalizedContent.length() > 500) {
            return CustomResponse.fail(400, "消息不能超过500字");
        }
        DmRoom room = dmRoomMapper.selectById(roomId);
        if (room == null || !isMember(room, userId)) {
            return CustomResponse.fail(404, "会话不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        DmMessage message = new DmMessage();
        message.setRoomId(roomId);
        message.setSenderId(userId);
        message.setContent(normalizedContent);
        message.setCreateTime(now);
        dmMessageMapper.insert(message);

        room.setUpdateTime(now);
        dmRoomMapper.updateById(room);

        Integer recipientId = peerId(room, userId);
        notificationService.notifyMessage(userId, recipientId, roomId, normalizedContent);
        UserDTO sender = userDirectoryClient.findById(userId);
        return CustomResponse.ok("发送成功", toMessageDTO(message, sender, userId));
    }

    private DmRoom findRoom(Integer userId, Integer peerId) {
        int userA = Math.min(userId, peerId);
        int userB = Math.max(userId, peerId);
        return dmRoomMapper.selectOne(new LambdaQueryWrapper<DmRoom>()
                .eq(DmRoom::getUserA, userA)
                .eq(DmRoom::getUserB, userB));
    }

    private DmRoomDTO buildRoomDTO(DmRoom room, Integer userId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 20 : Math.min(size, 100);
        Page<DmMessage> resultPage = new Page<>(safePage, safeSize);
        dmMessageMapper.selectPage(resultPage, new LambdaQueryWrapper<DmMessage>()
                .eq(DmMessage::getRoomId, room.getId())
                .orderByAsc(DmMessage::getCreateTime));

        Integer peerId = peerId(room, userId);
        Set<Integer> displayUserIds = new LinkedHashSet<>();
        displayUserIds.add(peerId);
        resultPage.getRecords().stream().map(DmMessage::getSenderId)
                .filter(Objects::nonNull).forEach(displayUserIds::add);
        Map<Integer, UserDTO> users = userDirectoryClient.findByIds(displayUserIds);

        List<DmMessageDTO> messages = new ArrayList<>();
        for (DmMessage message : resultPage.getRecords()) {
            messages.add(toMessageDTO(message, users.get(message.getSenderId()), userId));
        }

        UserDTO peer = users.get(peerId);
        DmRoomDTO dto = new DmRoomDTO();
        dto.setRoomId(room.getId());
        dto.setPeerId(peerId);
        dto.setPeerNickname(peer != null && StringUtils.hasText(peer.getNickname())
                ? peer.getNickname() : "用户");
        dto.setPeerAvatar(peer != null ? peer.getAvatar() : null);
        dto.setMessages(messages);
        return dto;
    }

    private static DmMessageDTO toMessageDTO(DmMessage message, UserDTO sender, Integer viewerId) {
        DmMessageDTO dto = new DmMessageDTO();
        dto.setId(message.getId());
        dto.setRoomId(message.getRoomId());
        dto.setSenderId(message.getSenderId());
        dto.setSenderNickname(sender != null && StringUtils.hasText(sender.getNickname())
                ? sender.getNickname() : "用户");
        dto.setSenderAvatar(sender != null ? sender.getAvatar() : null);
        dto.setContent(message.getContent());
        dto.setCreateTime(message.getCreateTime());
        dto.setMine(Objects.equals(message.getSenderId(), viewerId));
        return dto;
    }

    private static boolean isMember(DmRoom room, Integer userId) {
        return Objects.equals(room.getUserA(), userId) || Objects.equals(room.getUserB(), userId);
    }

    private static Integer peerId(DmRoom room, Integer userId) {
        return Objects.equals(room.getUserA(), userId) ? room.getUserB() : room.getUserA();
    }
}
