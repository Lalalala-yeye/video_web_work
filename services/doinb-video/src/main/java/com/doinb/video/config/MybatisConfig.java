package com.doinb.video.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.doinb.video.mapper")
public class MybatisConfig {
}
