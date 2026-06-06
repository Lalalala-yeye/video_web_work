package com.doinb.backend.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("play_history")
public class PlayHistory {
    private Integer userId;
    private Integer videoId;
    private Integer progress;
    private LocalDateTime updateTime;
}
