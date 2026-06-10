package com.doinb.backend.service.reaction;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.ReactionSummaryDTO;

import java.util.List;
import java.util.Map;

public interface ReactionService {

    ReactionSummaryDTO getVideoSummary(Integer videoId, Integer viewerUserId);

    CustomResponse setVideoReaction(Integer userId, Integer videoId, Integer reaction);

    ReactionSummaryDTO getCommentSummary(Integer commentId, Integer viewerUserId);

    Map<Integer, ReactionSummaryDTO> getCommentSummaries(List<Integer> commentIds, Integer viewerUserId);

    CustomResponse setCommentReaction(Integer userId, Integer commentId, Integer reaction);
}
