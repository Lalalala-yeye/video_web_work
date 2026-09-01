package com.doinb.interact.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("subscriptions")
public class Subscription {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer followerId;
    private Integer targetId;
    private LocalDateTime createTime;
}
