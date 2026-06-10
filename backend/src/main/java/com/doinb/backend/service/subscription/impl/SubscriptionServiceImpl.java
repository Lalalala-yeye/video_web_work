package com.doinb.backend.service.subscription.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.SubscriptionMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.FeedItemDTO;
import com.doinb.backend.pojo.dto.LiveRoomDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.Subscription;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.subscription.SubscriptionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final int STATUS_PUBLISHED = 1;
    private static final String DEMO_PLAY_PREFIX = "http://localhost:8080/live/play/";

    private final SubscriptionMapper subscriptionMapper;
    private final UserMapper userMapper;
    private final VideoMapper videoMapper;
    private final LiveRoomMapper liveRoomMapper;

    public SubscriptionServiceImpl(SubscriptionMapper subscriptionMapper,
                                   UserMapper userMapper,
                                   VideoMapper videoMapper,
                                   LiveRoomMapper liveRoomMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.userMapper = userMapper;
        this.videoMapper = videoMapper;
        this.liveRoomMapper = liveRoomMapper;
    }

    @Override
    public CustomResponse follow(Integer followerId, Integer targetId) {
        if (Objects.equals(followerId, targetId)) {
            return fail(400, "不能关注自己");
        }
        User target = userMapper.selectById(targetId);
        if (target == null) {
            return fail(404, "目标用户不存在");
        }

        Long count = subscriptionMapper.selectCount(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId)
                .eq(Subscription::getTargetId, targetId));
        if (count != null && count > 0) {
            return ok("已关注");
        }

        Subscription sub = new Subscription();
        sub.setFollowerId(followerId);
        sub.setTargetId(targetId);
        sub.setCreateTime(LocalDateTime.now());
        subscriptionMapper.insert(sub);
        return ok("关注成功");
    }

    @Override
    public CustomResponse unfollow(Integer followerId, Integer targetId) {
        int rows = subscriptionMapper.delete(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId)
                .eq(Subscription::getTargetId, targetId));
        if (rows == 0) {
            return ok("未关注");
        }
        return ok("已取消关注");
    }

    @Override
    public boolean isFollowing(Integer followerId, Integer targetId) {
        if (followerId == null || targetId == null) {
            return false;
        }
        Long count = subscriptionMapper.selectCount(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId)
                .eq(Subscription::getTargetId, targetId));
        return count != null && count > 0;
    }

    @Override
    public PageResult<UserDTO> listFollowing(Integer followerId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        Page<Subscription> mpPage = new Page<>(safePage, safeSize);
        subscriptionMapper.selectPage(mpPage, new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId)
                .orderByDesc(Subscription::getCreateTime));

        List<Integer> targetIds = mpPage.getRecords().stream()
                .map(Subscription::getTargetId)
                .collect(Collectors.toList());
        if (targetIds.isEmpty()) {
            return new PageResult<>(mpPage.getTotal(), safePage, safeSize, List.of());
        }

        Map<Integer, User> userMap = userMapper.selectBatchIds(targetIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<UserDTO> users = new ArrayList<>();
        for (Integer targetId : targetIds) {
            User user = userMap.get(targetId);
            if (user != null) {
                users.add(toUserDTO(user));
            }
        }
        return new PageResult<>(mpPage.getTotal(), safePage, safeSize, users);
    }

    @Override
    public PageResult<FeedItemDTO> feed(Integer followerId, long page, long size) {
        long safePage = page < 1 ? 1 : page;
        long safeSize = size < 1 ? 10 : Math.min(size, 50);

        List<Subscription> subs = subscriptionMapper.selectList(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId));
        if (subs.isEmpty()) {
            return new PageResult<>(0, safePage, safeSize, List.of());
        }

        List<Integer> targetIds = subs.stream()
                .map(Subscription::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .in(Video::getAuthorId, targetIds)
                .eq(Video::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Video::getCreateTime)
                .last("LIMIT 100"));

        List<LiveRoom> liveRooms = liveRoomMapper.selectList(new LambdaQueryWrapper<LiveRoom>()
                .in(LiveRoom::getAnchorId, targetIds)
                .eq(LiveRoom::getIsLive, true)
                .orderByDesc(LiveRoom::getId)
                .last("LIMIT 50"));

        Map<Integer, User> userMap = userMapper.selectBatchIds(targetIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<FeedItemDTO> items = new ArrayList<>();
        for (Video video : videos) {
            FeedItemDTO item = new FeedItemDTO();
            item.setType("video");
            item.setVideo(toVideoDTO(video, userMap.get(video.getAuthorId())));
            item.setSortTime(video.getCreateTime());
            items.add(item);
        }
        for (LiveRoom room : liveRooms) {
            FeedItemDTO item = new FeedItemDTO();
            item.setType("live");
            item.setLiveRoom(toLiveRoomDTO(room, userMap.get(room.getAnchorId())));
            item.setSortTime(LocalDateTime.now());
            items.add(item);
        }

        items.sort(Comparator.comparing(FeedItemDTO::getSortTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        long total = items.size();
        int from = (int) ((safePage - 1) * safeSize);
        if (from >= items.size()) {
            return new PageResult<>(total, safePage, safeSize, List.of());
        }
        int to = (int) Math.min(from + safeSize, items.size());
        return new PageResult<>(total, safePage, safeSize, items.subList(from, to));
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setNickname(user.getNickname());
        dto.setAvatar(user.getAvatar());
        dto.setRole(user.getRole());
        return dto;
    }

    private VideoDTO toVideoDTO(Video video, User author) {
        VideoDTO dto = new VideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setAuthorId(video.getAuthorId());
        dto.setAuthorNickname(author != null ? author.getNickname() : "未知作者");
        dto.setCoverUrl(video.getCoverUrl());
        dto.setVideoUrl(video.getVideoUrl());
        dto.setStatus(video.getStatus());
        dto.setCreateTime(video.getCreateTime());
        return dto;
    }

    private LiveRoomDTO toLiveRoomDTO(LiveRoom room, User anchor) {
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
