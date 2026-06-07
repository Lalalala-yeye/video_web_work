package com.doinb.backend.service.comment;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.CommentDTO;
import com.doinb.backend.pojo.dto.PageResult;

/** 评论业务 */
public interface CommentService {

    /** targetType: 1=视频 2=直播间 */
    CustomResponse add(Integer userId, Integer targetId, Integer targetType, String content);

    PageResult<CommentDTO> listByTarget(Integer targetId, Integer targetType, long page, long size);
}
