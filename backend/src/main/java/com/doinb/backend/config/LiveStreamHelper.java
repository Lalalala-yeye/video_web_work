package com.doinb.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 根据配置拼接 SRS 推流 / HLS 拉流地址 */
@Component
public class LiveStreamHelper {

    private final String playUrlPrefix;
    private final String pushUrlPrefix;

    public LiveStreamHelper(
            @Value("${live.play-url-prefix:http://127.0.0.1:8088/live/}") String playUrlPrefix,
            @Value("${live.push-url-prefix:rtmp://127.0.0.1:1935/live/}") String pushUrlPrefix) {
        this.playUrlPrefix = playUrlPrefix;
        this.pushUrlPrefix = pushUrlPrefix;
    }

    public String playUrl(String streamKey) {
        if (streamKey == null) {
            return null;
        }
        return playUrlPrefix + streamKey + ".m3u8";
    }

    /** OBS「服务器」字段：rtmp://host:1935/live */
    public String pushServer() {
        if (pushUrlPrefix.endsWith("/")) {
            return pushUrlPrefix.substring(0, pushUrlPrefix.length() - 1);
        }
        return pushUrlPrefix;
    }

    public String pushUrl(String streamKey) {
        if (streamKey == null) {
            return null;
        }
        return pushUrlPrefix + streamKey;
    }
}
