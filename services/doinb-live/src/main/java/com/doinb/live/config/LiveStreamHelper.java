package com.doinb.live.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 拼接直播媒体相对路径（不含主机名，由前端按访问域名代理播放） */
@Component
public class LiveStreamHelper {

    private final String mediaPathPrefix;

    public LiveStreamHelper(
            @Value("${live.media-path-prefix:/live/}") String mediaPathPrefix) {
        String prefix = mediaPathPrefix == null ? "/live/" : mediaPathPrefix.trim();
        this.mediaPathPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
    }

    /** 观众拉流相对路径，如 /live/{streamKey}.m3u8 */
    public String playUrl(String streamKey) {
        if (streamKey == null) {
            return null;
        }
        return mediaPathPrefix + streamKey + ".m3u8";
    }
}
