package com.doinb.backend.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
