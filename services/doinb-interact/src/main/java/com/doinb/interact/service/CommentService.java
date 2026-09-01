package com.doinb.interact.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.interact.pojo.dto.CommentDTO;

public interface CommentService {

    CustomResponse add(Integer userId, Integer targetId, Integer targetType, String content);

    PageResult<CommentDTO> listByTarget(Integer targetId, Integer targetType, long page, long size,
                                        Integer viewerUserId);
}
