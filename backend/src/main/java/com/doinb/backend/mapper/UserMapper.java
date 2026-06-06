package com.doinb.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doinb.backend.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM users WHERE username = #{username} LIMIT 1")
    User selectByUsername(@Param("username") String username);
}
