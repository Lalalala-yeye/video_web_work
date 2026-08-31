package com.doinb.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DoinbProperties.class)
public class CommonAutoConfig {
}
