package com.doinb.backend.pojo.dto;

import lombok.Data;

/** 对外展示的用户资料（不含账号等隐私） */
@Data
public class UserPublicDTO {
    private Integer id;
    private String nickname;
    private String avatar;
    private String bio;
}
