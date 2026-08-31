package com.doinb.message.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("dm_messages")
public class DmMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer roomId;
    private Integer senderId;
    private String content;
    private LocalDateTime createTime;
}
