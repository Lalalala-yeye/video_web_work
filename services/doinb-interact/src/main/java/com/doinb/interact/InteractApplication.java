package com.doinb.interact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.doinb.common", "com.doinb.interact"})
public class InteractApplication {

    public static void main(String[] args) {
        SpringApplication.run(InteractApplication.class, args);
    }
}
