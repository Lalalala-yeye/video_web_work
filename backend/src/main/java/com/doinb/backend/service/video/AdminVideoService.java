package com.doinb.backend.service.video;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.VideoDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 管理员视频审核
 */
public interface AdminVideoService {

    /** 待审核：新稿与修改后待审（status=0） */
    PageResult<VideoDTO> listPending(long page, long size);

    /** 举报待复审（status=2） */
    PageResult<VideoDTO> listReportReview(long page, long size);

    /** 审核通过 → 已发布 */
    CustomResponse approve(Integer adminRole, Integer videoId);

    /** 审核驳回 → 仅自己可见 */
    CustomResponse reject(Integer adminRole, Integer videoId);

    /** 管理员删除视频 */
    CustomResponse deleteVideo(Integer adminRole, Integer videoId);
}
