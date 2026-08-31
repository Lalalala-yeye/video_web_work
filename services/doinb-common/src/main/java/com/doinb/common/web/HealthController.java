package com.doinb.common.web;

import com.doinb.common.CustomResponse;
import com.doinb.common.config.DoinbProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final DoinbProperties properties;

    public HealthController(DoinbProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/health")
    public CustomResponse health() {
        return CustomResponse.ok(properties.getServiceName() + " ok");
    }
}
