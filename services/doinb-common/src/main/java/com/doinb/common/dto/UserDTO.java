package com.doinb.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Integer id;
    private String username;
    private String nickname;
    private String avatar;
    /** 0=普通用户  1=发布者  2=管理员 */
    private Integer role;
    private String bio;
}
