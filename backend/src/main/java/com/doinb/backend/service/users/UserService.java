package com.doinb.backend.service.users;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserDTO;

/**
 * 用户资料服务：查询、修改昵称和头像等（不含注册登录）。
 */
public interface UserService {

    /** 根据 id 查询用户公开信息（不含密码） */
    UserDTO getUserById(Integer id);

    /** 修改昵称和头像 URL */
    CustomResponse updateUserInfo(Integer userId, String nickname, String avatar);
}
