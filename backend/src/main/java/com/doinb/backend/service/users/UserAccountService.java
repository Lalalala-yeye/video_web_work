package com.doinb.backend.service.users;

import com.doinb.backend.pojo.CustomResponse;

/**
 * 账号相关服务：注册、登录、登出、改密码、获取当前登录用户信息。
 */
public interface UserAccountService {

    /** 用户注册 */
    CustomResponse register(String username, String password, String confirmedPassword);

    /** 普通用户登录（观众 / 发布者） */
    CustomResponse login(String username, String password);

    /** 管理员登录（role 必须为 2） */
    CustomResponse adminLogin(String username, String password);

    /** 获取当前登录用户的信息 */
    CustomResponse personalInfo();

    /** 获取当前登录管理员的信息 */
    CustomResponse adminPersonalInfo();

    /** 退出登录（无 Redis 时主要由前端删除 token） */
    CustomResponse logout();

    /** 修改密码 */
    CustomResponse updatePassword(String oldPassword, String newPassword);
}
