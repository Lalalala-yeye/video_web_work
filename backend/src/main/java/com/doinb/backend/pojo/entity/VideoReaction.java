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
@TableName("video_reactions")
public class VideoReaction {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer videoId;
    /** 1=赞 -1=踩 */
    private Integer reaction;
}
