package com.doinb.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 返回给前端的用户信息（不包含密码等敏感字段）
 */
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
    /** 个人简介（自己可见完整；公开展示页也展示） */
    private String bio;
}
