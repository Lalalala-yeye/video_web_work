package com.doinb.backend.service.search.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doinb.backend.config.LiveStreamHelper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.dto.LiveRoomDTO;
import com.doinb.backend.pojo.dto.SearchResultDTO;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.search.SearchService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private static final int STATUS_PUBLISHED = 1;

    private final VideoMapper videoMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;
    private final LiveStreamHelper liveStreamHelper;

    public SearchServiceImpl(VideoMapper videoMapper,
                             LiveRoomMapper liveRoomMapper,
                             UserMapper userMapper,
                             LiveStreamHelper liveStreamHelper) {
        this.videoMapper = videoMapper;
        this.liveRoomMapper = liveRoomMapper;
        this.userMapper = userMapper;
        this.liveStreamHelper = liveStreamHelper;
    }

    @Override
    public SearchResultDTO search(String keyword, long videoLimit, long liveLimit, long userLimit) {
        SearchResultDTO result = new SearchResultDTO();
        result.setVideos(List.of());
        result.setLiveRooms(List.of());
        result.setUsers(List.of());

        if (!StringUtils.hasText(keyword)) {
            return result;
        }

        String kw = keyword.trim();
        long vLimit = videoLimit < 1 ? 10 : Math.min(videoLimit, 50);
        long lLimit = liveLimit < 1 ? 10 : Math.min(liveLimit, 50);
        long uLimit = userLimit < 1 ? 10 : Math.min(userLimit, 50);

        List<Video> videos = videoMapper.selectList(new LambdaQueryWrapper<Video>()
                .eq(Video::getStatus, STATUS_PUBLISHED)
                .like(Video::getTitle, kw)
                .orderByDesc(Video::getCreateTime)
                .last("LIMIT " + vLimit));
        result.setVideos(toVideoDTOList(videos));

        List<LiveRoom> rooms = liveRoomMapper.selectList(new LambdaQueryWrapper<LiveRoom>()
                .like(LiveRoom::getTitle, kw)
                .orderByDesc(LiveRoom::getId)
                .last("LIMIT " + lLimit));
        result.setLiveRooms(toLiveRoomDTOList(rooms));

        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw))
                .orderByDesc(User::getId)
                .last("LIMIT " + uLimit));
        result.setUsers(toUserDTOList(users));

        return result;
    }

    private List<VideoDTO> toVideoDTOList(List<Video> videos) {
        if (videos.isEmpty()) {
            return List.of();
        }
        List<Integer> authorIds = videos.stream()
                .map(Video::getAuthorId)
                .distinct()
                .collect(Collectors.toList());
        Map<Integer, User> authorMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<VideoDTO> list = new ArrayList<>();
        for (Video video : videos) {
            User author = authorMap.get(video.getAuthorId());
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
            list.add(dto);
        }
        return list;
    }

    private List<LiveRoomDTO> toLiveRoomDTOList(List<LiveRoom> rooms) {
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
            User anchor = anchorMap.get(room.getAnchorId());
            LiveRoomDTO dto = new LiveRoomDTO();
            dto.setId(room.getId());
            dto.setTitle(room.getTitle());
            dto.setAnchorId(room.getAnchorId());
            dto.setAnchorNickname(anchor != null ? anchor.getNickname() : "未知主播");
            dto.setIsLive(room.getIsLive());
            if (Boolean.TRUE.equals(room.getIsLive()) && room.getStreamKey() != null) {
                dto.setPlayUrl(liveStreamHelper.playUrl(room.getStreamKey()));
            }
            list.add(dto);
        }
        return list;
    }

    private List<UserDTO> toUserDTOList(List<User> users) {
        List<UserDTO> list = new ArrayList<>();
        for (User user : users) {
            UserDTO dto = new UserDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setAvatar(user.getAvatar());
            dto.setRole(user.getRole());
            list.add(dto);
        }
        return list;
    }
}
