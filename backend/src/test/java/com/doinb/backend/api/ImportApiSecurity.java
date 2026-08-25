package com.doinb.backend.api;

import com.doinb.backend.config.GlobalExceptionHandler;
import com.doinb.backend.config.SecurityConfig;
import com.doinb.backend.config.filter.JwtAuthenticationTokenFilter;
import com.doinb.backend.service.utils.CurrentUser;
import com.doinb.backend.utils.JwtUtil;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MockMvc 集成测共用：走真实 Security + JWT Filter，不启动 MySQL。
 * 每个测试类还要 {@code @MockitoBean}：UserDetailsService、UserMapper、UserService。
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({
        SecurityConfig.class,
        JwtAuthenticationTokenFilter.class,
        JwtUtil.class,
        CurrentUser.class,
        GlobalExceptionHandler.class
})
public @interface ImportApiSecurity {
}
