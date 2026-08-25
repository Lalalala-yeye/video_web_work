package com.doinb.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 全量启动测试（需要本机 MySQL 与 {@code application-local.yml}）。
 * <p>
 * 测试报告中的业务用例不要写在这里，按模块放在对应 {@code *ServiceImplTest}：
 * <ul>
 *   <li>U000～U081 → {@code UserAccountServiceImplTest} / {@code UserServiceImplTest}</li>
 *   <li>V011/V030/V031/V050/V061/V063 → {@code VideoServiceImplTest}</li>
 *   <li>C000/C002/L040/L041 → {@code CommentServiceImplTest}</li>
 *   <li>R000/R001/R010 → {@code ReactionServiceImplTest}</li>
 *   <li>F000/F001/F010 → {@code SubscriptionServiceImplTest}</li>
 *   <li>S000/S001/S002 → {@code SearchServiceImplTest}</li>
 *   <li>L010/L020/L021/L022 → {@code LiveRoomServiceImplTest}</li>
 *   <li>M000/M010/M020 → {@code MessageServiceImplTest}</li>
 *   <li>N010/N021 → {@code NotificationServiceImplTest}</li>
 *   <li>A001/A010/A011/A040 → {@code AdminVideoServiceImplTest}</li>
 * </ul>
 */
@SpringBootTest
class BackendApplicationTests {

	@Test
	@Disabled("需要本机 MySQL；业务断言见 src/test/.../*ServiceImplTest")
	void contextLoads() {
	}

}
