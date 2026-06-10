package com.doinb.backend.service.video;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.PlayHistoryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频模块：列表、播放、历史、上传
 */
public interface VideoService {

    /** 已发布视频分页列表（游客可看） */
    PageResult<VideoDTO> listPublished(long page, long size);

    /** 视频详情（已发布公开；作者/管理员可看待审与私密） */
    CustomResponse getOne(Integer videoId, Integer viewerUserId, Integer viewerRole);

    /** 记录/更新播放进度（需登录） */
    CustomResponse saveProgress(Integer userId, Integer videoId, Integer progress);

    /** 当前用户的播放历史（需登录） */
    PageResult<PlayHistoryDTO> listHistory(Integer userId, long page, long size);

    /**
     * 发布者上传视频
     * @param visibility public=他人可见(待审核) private=仅自己可见
     */
    CustomResponse upload(Integer userId, Integer role, String title, String description,
                          String visibility, MultipartFile cover, MultipartFile videoFile);

    /** 我上传的视频列表（含各状态） */
    PageResult<VideoDTO> listMyVideos(Integer userId, long page, long size);

    /** 编辑视频元数据（可选更换封面/视频文件）；他人可见时重新进入待审核 */
    CustomResponse updateVideo(Integer userId, Integer role, Integer videoId,
                             String title, String description, String visibility,
                             MultipartFile cover, MultipartFile videoFile);

    /** 获取本人可管理的视频详情（含未发布） */
    CustomResponse getMyVideo(Integer userId, Integer role, Integer videoId);

    /** 发布者设置可见性：public/private（不能自行设为已发布） */
    CustomResponse setVisibility(Integer userId, Integer role, Integer videoId, String visibility);

    /** 举报视频（已发布视频累计达阈值进入复审） */
    CustomResponse reportVideo(Integer userId, Integer videoId, String reason);

    /** 删除视频（发布者本人） */
    CustomResponse deleteVideo(Integer userId, Integer role, Integer videoId);
}
