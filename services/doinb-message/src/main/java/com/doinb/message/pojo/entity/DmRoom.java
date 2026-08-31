package com.doinb.message.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dm_rooms")
public class DmRoom {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userA;
    private Integer userB;
    private LocalDateTime updateTime;
}
