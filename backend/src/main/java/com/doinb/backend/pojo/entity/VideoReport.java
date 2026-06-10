package com.doinb.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("video_reports")
public class VideoReport {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer videoId;
    private Integer reporterId;
    private String reason;
    private LocalDateTime createTime;
}
