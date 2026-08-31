package com.doinb.user.pojo.dto;

import com.doinb.common.dto.VideoDTO;
import lombok.Data;

import java.util.List;

@Data
public class UserShowcaseDTO {
    private UserPublicDTO profile;
    private List<VideoDTO> videos;
    private long videoTotal;
    private Boolean following;
}
