package com.doinb.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doinb.backend.pojo.entity.PlayHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PlayHistoryMapper extends BaseMapper<PlayHistory> {
}
