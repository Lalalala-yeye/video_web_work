package com.doinb.live.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doinb.live.mapper.LiveRoomMapper;
import com.doinb.live.pojo.LiveRoom;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 直播房间维护：停播后超时删除房间；长期未开播房间清理。
 * 弹幕清零已归互动服务，本任务只管 live_rooms。
 */
@Component
public class LiveMaintenanceTask {

    /** 停播后保留时长（小时），超时删除房间记录 */
    private static final int ENDED_RETENTION_HOURS = 2;
    /** 创建后从未开播的保留时长（小时） */
    private static final int NEVER_STARTED_RETENTION_HOURS = 24;

    private final LiveRoomMapper liveRoomMapper;

    public LiveMaintenanceTask(LiveRoomMapper liveRoomMapper) {
        this.liveRoomMapper = liveRoomMapper;
    }

    /** 每 30 分钟清理过期直播间 */
    @Scheduled(cron = "0 */30 * * * ?")
    public void purgeIdleRooms() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endedCutoff = now.minusHours(ENDED_RETENTION_HOURS);
        LocalDateTime neverStartedCutoff = now.minusHours(NEVER_STARTED_RETENTION_HOURS);

        // 停播超过保留时长的房间删除
        liveRoomMapper.delete(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, false)
                .isNotNull(LiveRoom::getEndedAt)
                .lt(LiveRoom::getEndedAt, endedCutoff));

        // 创建后从未开播且超时的房间删除
        liveRoomMapper.delete(new LambdaQueryWrapper<LiveRoom>()
                .eq(LiveRoom::getIsLive, false)
                .isNull(LiveRoom::getEndedAt)
                .isNull(LiveRoom::getSessionStart)
                .lt(LiveRoom::getCreateTime, neverStartedCutoff));
    }
}
