package com.doinb.video.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
