package com.doinb.interact.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.doinb.interact.pojo.entity.Comment;
import com.doinb.interact.pojo.entity.CommentReaction;
import com.doinb.interact.pojo.entity.Subscription;
import com.doinb.interact.pojo.entity.VideoReaction;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/** 单测中让 LambdaQueryWrapper 可用（不连数据库）。 */
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
            TableInfoHelper.initTableInfo(assistant, Comment.class);
            TableInfoHelper.initTableInfo(assistant, CommentReaction.class);
            TableInfoHelper.initTableInfo(assistant, VideoReaction.class);
            TableInfoHelper.initTableInfo(assistant, Subscription.class);
            initialized = true;
        }
    }
}
