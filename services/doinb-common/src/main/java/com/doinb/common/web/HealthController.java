package com.doinb.common.web;

import com.doinb.common.CustomResponse;
import com.doinb.common.config.DoinbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final DoinbProperties properties;
    private final DataSource dataSource;
    private final String appVersion;

    public HealthController(DoinbProperties properties,
                            @Autowired(required = false) DataSource dataSource,
                            @Value("${APP_VERSION:dev}") String appVersion) {
        this.properties = properties;
        this.dataSource = dataSource;
        this.appVersion = appVersion;
    }

    /** 存活：进程起来即可。K8s liveness / Docker HEALTHCHECK。 */
    @GetMapping("/health")
    public CustomResponse health() {
        return CustomResponse.ok(properties.getServiceName() + " ok");
    }

    /** 就绪：有库则探一下连接。K8s readiness。 */
    @GetMapping("/ready")
    public ResponseEntity<CustomResponse> ready() {
        if (dataSource != null) {
            try (Connection c = dataSource.getConnection()) {
                if (!c.isValid(2)) {
                    return ResponseEntity.status(503).body(CustomResponse.fail(503, "db not ready"));
                }
            } catch (Exception e) {
                return ResponseEntity.status(503).body(CustomResponse.fail(503, "db not ready"));
            }
        }
        return ResponseEntity.ok(CustomResponse.ok("ready"));
    }

    /** 版本：镜像构建时写入 APP_VERSION（git 短 SHA）。 */
    @GetMapping("/version")
    public CustomResponse version() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("service", properties.getServiceName());
        data.put("version", appVersion);
        return CustomResponse.ok(data);
    }
}
