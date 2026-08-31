package com.doinb.video.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.VideoDTO;
import com.doinb.video.pojo.dto.PlayHistoryDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface VideoService {

    PageResult<VideoDTO> listPublished(long page, long size);

    CustomResponse getOne(Integer videoId, Integer viewerUserId, Integer viewerRole);

    CustomResponse saveProgress(Integer userId, Integer videoId, Integer progress);

    PageResult<PlayHistoryDTO> listHistory(Integer userId, long page, long size);

    CustomResponse upload(Integer userId, Integer role, String title, String description,
                          String visibility, MultipartFile cover, MultipartFile videoFile);

    PageResult<VideoDTO> listMyVideos(Integer userId, long page, long size);

    CustomResponse updateVideo(Integer userId, Integer role, Integer videoId,
                               String title, String description, String visibility,
                               MultipartFile cover, MultipartFile videoFile);

    CustomResponse getMyVideo(Integer userId, Integer role, Integer videoId);

    CustomResponse setVisibility(Integer userId, Integer role, Integer videoId, String visibility);

    CustomResponse reportVideo(Integer userId, Integer videoId, String reason);

    CustomResponse deleteVideo(Integer userId, Integer role, Integer videoId);

    VideoDTO getInternal(Integer videoId);

    List<VideoDTO> listPublishedByAuthors(List<Integer> authorIds, long limit);

    List<VideoDTO> searchPublished(String keyword, long limit);
}
