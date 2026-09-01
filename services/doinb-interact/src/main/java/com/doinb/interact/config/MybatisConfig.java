package com.doinb.interact.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.doinb.interact.mapper")
public class MybatisConfig {
}
