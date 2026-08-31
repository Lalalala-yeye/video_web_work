package com.doinb.message.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notifications")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer type;
    private Integer actorId;
    private Integer refId;
    private String preview;
    private String linkPath;
    private Boolean isRead;
    private LocalDateTime createTime;
}
