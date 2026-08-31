package com.doinb.video.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.VideoDTO;
import com.doinb.video.pojo.dto.VideoReportDTO;

import java.util.List;

public interface AdminVideoService {

    PageResult<VideoDTO> listPending(Integer adminRole, long page, long size);

    PageResult<VideoDTO> listReportReview(Integer adminRole, long page, long size);

    List<VideoReportDTO> listReports(Integer adminRole, Integer videoId);

    CustomResponse getVideoForPreview(Integer adminRole, Integer videoId);

    CustomResponse approve(Integer adminRole, Integer adminUserId, Integer videoId);

    CustomResponse reject(Integer adminRole, Integer videoId);

    CustomResponse deleteVideo(Integer adminRole, Integer videoId);
}
