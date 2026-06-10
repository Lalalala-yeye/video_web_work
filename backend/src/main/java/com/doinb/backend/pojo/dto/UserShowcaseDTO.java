package com.doinb.backend.pojo.dto;

import lombok.Data;

import java.util.List;

/** 用户公开展示页 */
@Data
public class UserShowcaseDTO {
    private UserPublicDTO profile;
    private List<VideoDTO> videos;
    private long videoTotal;
    /** 当前登录用户是否已关注该用户（未登录时为 false） */
    private Boolean following;
}
