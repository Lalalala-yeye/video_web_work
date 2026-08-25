package com.doinb.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * 全量启动测试（需要本机 MySQL 与 {@code application-local.yml}）。
 * <p>
 * 不要加 {@code @SpringBootTest}：方法上 {@code @Disabled} 仍会先启动上下文，
 * CI / 无库环境会失败。业务断言见 {@code *ServiceImplTest}；接口断言见 {@code api/*ApiTest}。
 */
@Disabled("需要本机 MySQL 与 application-local.yml，不在 mvn test / CI 运行")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
