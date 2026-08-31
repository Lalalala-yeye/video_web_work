package com.doinb.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "doinb")
public class DoinbProperties {

    /** gateway 或 service */
    private String role = "service";
    private String serviceName = "doinb";
    private String internalToken = "doinb-internal-dev-token";
    private List<String> publicPathPrefixes = new ArrayList<>();
    private Services services = new Services();

    @Data
    public static class Services {
        private String user = "http://127.0.0.1:8082";
        private String video = "http://127.0.0.1:8083";
        private String live = "http://127.0.0.1:8084";
        private String interact = "http://127.0.0.1:8085";
        private String message = "http://127.0.0.1:8086";
    }
}
