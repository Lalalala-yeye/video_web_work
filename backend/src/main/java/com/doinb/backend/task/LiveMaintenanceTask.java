package com.doinb.backend.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.LiveRoom;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 直播维护：每日清零弹幕；停播后超时删除房间；长期未开播房间清理。
 */
@Component
public class LiveMaintenanceTask {

    private static final int TARGET_LIVE = 2;
    /** 停播后保留时长（小时），超时删除房间记录 */
    private static final int ENDED_RETENTION_HOURS = 2;
    /** 创建后从未开播的保留时长（小时） */
    private static final int NEVER_STARTED_RETENTION_HOURS = 24;

    private final CommentMapper commentMapper;
    private final LiveRoomMapper liveRoomMapper;

    public LiveMaintenanceTask(CommentMapper commentMapper, LiveRoomMapper liveRoomMapper) {
        this.commentMapper = commentMapper;
        this.liveRoomMapper = liveRoomMapper;
    }

    /** 每天 0:05 清空全部直播弹幕 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void clearDailyLiveComments() {
        commentMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getTargetType, TARGET_LIVE));
    }

    /** 每 30 分钟清理过期直播间 */
    @Scheduled(cron = "0 */30 * * * ?")
    public void purgeIdleRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endedCutoff = now.minusHours(ENDED_RETENTION_HOURS);
        LocalDateTime neverStartedCutoff = now.minusHours(NEVER_STARTED_RETENTION_HOURS);

        liveRoomMapper.delete(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, false)
                .isNotNull(LiveRoom::getEndedAt)
                .lt(LiveRoom::getEndedAt, endedCutoff));

        liveRoomMapper.delete(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, false)
                .isNull(LiveRoom::getEndedAt)
                .isNull(LiveRoom::getSessionStart)
                .lt(LiveRoom::getCreateTime, neverStartedCutoff));
    }
}
