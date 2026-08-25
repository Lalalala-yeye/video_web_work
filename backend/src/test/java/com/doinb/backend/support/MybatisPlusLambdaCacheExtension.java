package com.doinb.backend.support;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.CommentReaction;
import com.doinb.backend.pojo.entity.DmMessage;
import com.doinb.backend.pojo.entity.DmRoom;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.Notification;
import com.doinb.backend.pojo.entity.PlayHistory;
import com.doinb.backend.pojo.entity.Subscription;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.pojo.entity.VideoReaction;
import com.doinb.backend.pojo.entity.VideoReport;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * 纯 Mockito 单测也会 new {@code LambdaUpdateWrapper}，需要 MyBatis-Plus 的表缓存。
 * 以前靠 {@code @SpringBootTest} 副作用初始化；去掉全量启动后改由本扩展补上。
 */
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
            Class<?>[] entities = {
                    User.class, Video.class, LiveRoom.class, Notification.class, VideoReport.class,
                    Comment.class, Subscription.class, PlayHistory.class, DmRoom.class, DmMessage.class,
                    VideoReaction.class, CommentReaction.class
            };
            for (Class<?> entity : entities) {
                TableInfoHelper.initTableInfo(assistant, entity);
            }
            initialized = true;
        }
    }
}
