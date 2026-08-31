package com.doinb.user.service;

import com.doinb.common.CustomResponse;

public interface UserAccountService {

    CustomResponse register(String username, String password, String confirmedPassword);

    CustomResponse login(String username, String password);

    CustomResponse adminLogin(String username, String password);

    CustomResponse personalInfo(Integer userId);

    CustomResponse adminPersonalInfo(Integer userId, boolean admin);

    CustomResponse logout();

    CustomResponse updatePassword(Integer userId, String oldPassword, String newPassword);
}
