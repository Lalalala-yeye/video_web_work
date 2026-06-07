package com.doinb.backend.service.video;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.PlayHistoryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频模块：列表、播放、历史、上传（M2 + 基础上传）
 */
public interface VideoService {

    /** 已发布视频分页列表（游客可看） */
    PageResult<VideoDTO> listPublished(long page, long size);

    /** 视频详情与播放地址（仅已发布；游客可看） */
    CustomResponse getOne(Integer videoId);

    /** 记录/更新播放进度（需登录） */
    CustomResponse saveProgress(Integer userId, Integer videoId, Integer progress);

    /** 当前用户的播放历史（需登录） */
    PageResult<PlayHistoryDTO> listHistory(Integer userId, long page, long size);

    /** 发布者上传视频（需登录且 role 为发布者或管理员） */
    CustomResponse upload(Integer userId, Integer role, String title, String description,
                          MultipartFile cover, MultipartFile videoFile);

    /** 我上传的视频列表（含各状态） */
    PageResult<VideoDTO> listMyVideos(Integer userId, long page, long size);

    /** 编辑视频元数据 */
    CustomResponse updateVideo(Integer userId, Integer role, Integer videoId,
                             String title, String description);

    /** 修改视频状态：0审核中 1已发布 2已下架 */
    CustomResponse updateStatus(Integer userId, Integer role, Integer videoId, Integer status);

    /** 删除视频 */
    CustomResponse deleteVideo(Integer userId, Integer role, Integer videoId);
}
