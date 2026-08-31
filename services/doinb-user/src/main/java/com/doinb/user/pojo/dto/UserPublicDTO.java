package com.doinb.user.pojo.dto;

import lombok.Data;

@Data
public class UserPublicDTO {
    private Integer id;
    private String nickname;
    private String avatar;
    private String bio;
}
