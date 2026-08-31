package com.doinb.video.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.doinb.video.pojo.entity.PlayHistory;
import com.doinb.video.pojo.entity.Video;
import com.doinb.video.pojo.entity.VideoReport;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class MybatisPlusLambdaCacheExtension implements BeforeAllCallback {

    private static final Object LOCK = new Object();
    private static volatile boolean initialized;

    @Override
    public void beforeAll(ExtensionContext context) {
        if (initialized) {
            return;
        }
        synchronized (LOCK) {
            if (initialized) {
                return;
            }
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setMapUnderscoreToCamelCase(true);
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
            TableInfoHelper.initTableInfo(assistant, Video.class);
            TableInfoHelper.initTableInfo(assistant, PlayHistory.class);
            TableInfoHelper.initTableInfo(assistant, VideoReport.class);
            initialized = true;
        }
    }
}
