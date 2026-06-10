package com.doinb.backend.service.users;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.UserDTO;
import com.doinb.backend.pojo.dto.UserPublicDTO;
import com.doinb.backend.pojo.dto.UserShowcaseDTO;
import com.doinb.backend.pojo.entity.User;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户资料服务：查询、修改昵称和头像等（不含注册登录）。
 */
public interface UserService {

    UserDTO getUserById(Integer id);

    UserPublicDTO getPublicProfile(Integer id);

    UserShowcaseDTO getShowcase(Integer id, long page, long size);

    void ensurePublisherRole(User user);

    CustomResponse updateUserInfo(Integer userId, String nickname, String bio);

    CustomResponse uploadAvatar(Integer userId, MultipartFile file);
}
