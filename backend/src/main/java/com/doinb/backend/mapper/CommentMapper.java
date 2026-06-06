package com.doinb.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doinb.backend.pojo.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
