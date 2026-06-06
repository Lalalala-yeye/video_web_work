package com.doinb.backend.service.users.impl;

import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.pojo.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 登录时会调用这里：根据用户名从数据库查用户。
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return new UserDetailsImpl(user);
    }
}
