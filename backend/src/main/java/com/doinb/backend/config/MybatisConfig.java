package com.doinb.backend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 单独放 MapperScan，避免写在 {@code BackendApplication} 上。
 * {@code @WebMvcTest} 不会加载本配置，因此接口集成测不必起 MySQL。
 */
@Configuration
@MapperScan("com.doinb.backend.mapper")
public class MybatisConfig {
}
