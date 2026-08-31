package com.doinb.user.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.dto.UserDTO;
import com.doinb.user.pojo.dto.UserPublicDTO;
import com.doinb.user.pojo.dto.UserShowcaseDTO;
import com.doinb.user.pojo.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService {

    UserDTO getUserById(Integer id);

    UserPublicDTO getPublicProfile(Integer id);

    UserShowcaseDTO getShowcase(Integer id, long page, long size);

    List<UserDTO> listByIds(List<Integer> ids);

    List<UserDTO> search(String keyword, long limit);

    void ensurePublisherRole(User user);

    CustomResponse updateUserInfo(Integer userId, String nickname, String bio);

    CustomResponse uploadAvatar(Integer userId, MultipartFile file);
}
