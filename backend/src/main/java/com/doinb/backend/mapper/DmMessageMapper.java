package com.doinb.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doinb.backend.pojo.entity.DmMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DmMessageMapper extends BaseMapper<DmMessage> {
}
