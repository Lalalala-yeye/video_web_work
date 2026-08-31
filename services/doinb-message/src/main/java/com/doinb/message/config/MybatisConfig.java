package com.doinb.message.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.doinb.message.mapper")
public class MybatisConfig {
}
