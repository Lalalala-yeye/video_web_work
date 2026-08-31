package com.doinb.live.controller;

import com.doinb.common.CustomResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public CustomResponse health() {
        return CustomResponse.ok("OK", "doinb-live ok");
    }
}
