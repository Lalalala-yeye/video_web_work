package com.doinb.backend.service.utils;

import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.service.users.impl.UserDetailsImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 工具类：从 Spring Security 上下文里取出「当前登录用户是谁」。
 * 在 Service 里需要知道 userId 时，注入本类并调用 getUserId() 即可。
 */
@Component
public class CurrentUser {

    /**
     * 获取当前登录用户的 id。
     * 若未登录或 token 无效，会抛出异常。
     */
    public Integer getUserId() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        User user = loginUser.getUser();
        return user.getId();
    }

    /** 判断当前用户是否是管理员（role == 2） */
    public boolean isAdmin() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl loginUser = (UserDetailsImpl) authentication.getPrincipal();
        return loginUser.getUser().getRole() != null && loginUser.getUser().getRole() == 2;
    }
}
