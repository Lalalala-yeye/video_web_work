package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public CustomResponse health() {
        CustomResponse resp = new CustomResponse();
        resp.setData("doinb-backend ok");
        return resp;
    }
}
