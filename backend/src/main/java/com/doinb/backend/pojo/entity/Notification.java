package com.doinb.backend.pojo.entity;

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
    /** 接收者 */
    private Integer userId;
    /** 1=视频被赞 2=评论被赞 3=私信 */
    private Integer type;
    /** 触发者 */
    private Integer actorId;
    /** 关联 id：videoId / commentId / roomId */
    private Integer refId;
    private String preview;
    private Boolean isRead;
    private LocalDateTime createTime;
}
