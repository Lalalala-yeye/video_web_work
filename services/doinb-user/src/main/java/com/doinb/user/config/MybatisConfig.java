package com.doinb.user.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.doinb.user.mapper")
public class MybatisConfig {
}
