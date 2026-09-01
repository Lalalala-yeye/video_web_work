package com.doinb.interact.pojo.dto;

import lombok.Data;

/** 点赞/点踩统计与当前用户态度 */
@Data
public class ReactionSummaryDTO {
    private long likeCount;
    private long dislikeCount;
    /** 当前用户：1赞 -1踩 0无 */
    private Integer userReaction;
}
