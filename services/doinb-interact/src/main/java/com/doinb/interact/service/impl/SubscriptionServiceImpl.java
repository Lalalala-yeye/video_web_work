package com.doinb.interact.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.dto.VideoDTO;
import com.doinb.interact.client.LiveDirectory;
import com.doinb.interact.client.UserDirectory;
import com.doinb.interact.client.VideoDirectory;
import com.doinb.interact.mapper.SubscriptionMapper;
import com.doinb.interact.pojo.dto.FeedItemDTO;
import com.doinb.interact.pojo.entity.Subscription;
import com.doinb.interact.service.SubscriptionService;
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

    private final SubscriptionMapper subscriptionMapper;
    private final UserDirectory userDirectory;
    private final VideoDirectory videoDirectory;
    private final LiveDirectory liveDirectory;

    public SubscriptionServiceImpl(SubscriptionMapper subscriptionMapper,
                                   UserDirectory userDirectory,
                                   VideoDirectory videoDirectory,
                                   LiveDirectory liveDirectory) {
        this.subscriptionMapper = subscriptionMapper;
        this.userDirectory = userDirectory;
        this.videoDirectory = videoDirectory;
        this.liveDirectory = liveDirectory;
    }

    @Override
    public CustomResponse follow(Integer followerId, Integer targetId) {
        if (Objects.equals(followerId, targetId)) {
            return CustomResponse.fail(400, "不能关注自己");
        }
        if (userDirectory.findById(targetId) == null) {
            return CustomResponse.fail(404, "目标用户不存在");
        }

        Long count = subscriptionMapper.selectCount(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId)
                .eq(Subscription::getTargetId, targetId));
        if (count != null && count > 0) {
            return CustomResponse.ok("已关注", null);
        }

        Subscription sub = new Subscription();
        sub.setFollowerId(followerId);
        sub.setTargetId(targetId);
        sub.setCreateTime(LocalDateTime.now());
        subscriptionMapper.insert(sub);
        return CustomResponse.ok("关注成功", null);
    }

    @Override
    public CustomResponse unfollow(Integer followerId, Integer targetId) {
        int rows = subscriptionMapper.delete(new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getFollowerId, followerId)
                .eq(Subscription::getTargetId, targetId));
        if (rows == 0) {
            return CustomResponse.ok("未关注", null);
        }
        return CustomResponse.ok("已取消关注", null);
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

        Map<Integer, UserDTO> userMap = userDirectory.findByIds(targetIds);
        List<UserDTO> users = new ArrayList<>();
        for (Integer targetId : targetIds) {
            UserDTO user = userMap.get(targetId);
            if (user != null) {
                users.add(user);
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

        List<VideoDTO> videos = videoDirectory.listPublishedByAuthors(targetIds, 100);
        List<LiveRoomDTO> liveRooms = liveDirectory.listLiveByAnchors(targetIds, 50);

        Map<Integer, UserDTO> userMap = userDirectory.findByIds(targetIds);

        List<FeedItemDTO> items = new ArrayList<>();
        for (VideoDTO video : videos) {
            FeedItemDTO item = new FeedItemDTO();
            item.setType("video");
            item.setVideo(video);
            item.setSortTime(video.getCreateTime());
            items.add(item);
        }
        for (LiveRoomDTO room : liveRooms) {
            FeedItemDTO item = new FeedItemDTO();
            item.setType("live");
            item.setLiveRoom(room);
            item.setSortTime(LocalDateTime.now());
            UserDTO anchor = userMap.get(room.getAnchorId());
            if (anchor != null && room.getAnchorNickname() == null) {
                room.setAnchorNickname(anchor.getNickname());
            }
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
}
