package com.doinb.backend.config.filter;

import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationTokenFilter {
    // 登录模块开发时再继承 OncePerRequestFilter 并实现鉴权逻辑
}
