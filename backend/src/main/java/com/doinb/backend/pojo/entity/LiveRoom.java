package com.doinb.backend.pojo.entity;

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
@TableName("live_rooms")
public class LiveRoom {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String title;
    private Integer anchorId;
    private String streamKey;
    private Boolean isLive;
    /** 本场开播时间，用于过滤本场弹幕 */
    private LocalDateTime sessionStart;
    /** 停播时间，用于定时清理房间 */
    private LocalDateTime endedAt;
    private LocalDateTime createTime;
}
