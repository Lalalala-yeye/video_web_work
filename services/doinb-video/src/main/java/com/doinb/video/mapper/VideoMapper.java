package com.doinb.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.doinb.video.pojo.entity.Video;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface VideoMapper extends BaseMapper<Video> {

    @Delete("DELETE FROM play_history WHERE video_id = #{videoId}")
    int deleteHistoryByVideoId(@Param("videoId") Integer videoId);

    @Delete("DELETE FROM video_reports WHERE video_id = #{videoId}")
    int deleteReportsByVideoId(@Param("videoId") Integer videoId);

    @Delete("DELETE FROM video_reactions WHERE video_id = #{videoId}")
    int deleteReactionsByVideoId(@Param("videoId") Integer videoId);
}
