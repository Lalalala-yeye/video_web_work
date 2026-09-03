package com.doinb.video.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.util.stream.Stream;

/**
 * k8s 的 uploads 是 emptyDir，没有 compose 那份宿主机目录。
 * 启动时把镜像里的演示样片拷到 UPLOAD_PATH，并幂等写入 seed 账号/视频。
 */
@Component
@Order(0)
public class DemoContentInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoContentInitializer.class);

    private final DataSource dataSource;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    public DemoContentInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        copyDemoMedia();
        applySeedWithRetry();
    }

    private void copyDemoMedia() {
        Path destRoot = Path.of(uploadPath).toAbsolutePath().normalize();
        Path source = findDemoMedia();
        if (source == null) {
            log.warn("未找到演示媒体目录（/opt/demo-media 或 backend/demo-media），首页封面可能为空");
            return;
        }
        try {
            copyTree(source.resolve("videos"), destRoot.resolve("videos"));
            copyTree(source.resolve("covers"), destRoot.resolve("covers"));
            Files.createDirectories(destRoot.resolve("avatars"));
            log.info("已写入演示封面和视频: {} -> {}", source, destRoot);
        } catch (IOException e) {
            log.warn("拷贝演示媒体失败: {}", e.getMessage());
        }
    }

    private static Path findDemoMedia() {
        Path[] candidates = {
                Path.of("/opt/demo-media"),
                Path.of("backend/demo-media"),
                Path.of("../backend/demo-media"),
        };
        for (Path p : candidates) {
            if (Files.isDirectory(p.resolve("videos")) && Files.isDirectory(p.resolve("covers"))) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static void copyTree(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) {
            return;
        }
        Files.createDirectories(to);
        try (Stream<Path> walk = Files.walk(from)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                Path target = to.resolve(from.relativize(file).toString());
                try {
                    Files.createDirectories(target.getParent());
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private void applySeedWithRetry() {
        Resource seed = new ClassPathResource("db/seed.sql");
        if (!seed.exists()) {
            log.warn("classpath 没有 db/seed.sql，跳过演示账号写入");
            return;
        }
        Exception last = null;
        for (int i = 1; i <= 20; i++) {
            try (Connection connection = dataSource.getConnection()) {
                ScriptUtils.executeSqlScript(connection, seed);
                log.info("已写入演示账号和视频记录（demo_admin / demo_author / demo_user，密码 123456）");
                return;
            } catch (Exception e) {
                last = e;
                log.info("等待数据库可执行 seed（{}/20）: {}", i, e.getMessage());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.warn("演示数据 seed 未成功: {}", last == null ? "" : last.getMessage());
    }
}
