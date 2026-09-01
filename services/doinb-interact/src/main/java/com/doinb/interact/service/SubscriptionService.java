package com.doinb.interact.service;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.UserDTO;
import com.doinb.interact.pojo.dto.FeedItemDTO;

public interface SubscriptionService {

    CustomResponse follow(Integer followerId, Integer targetId);

    CustomResponse unfollow(Integer followerId, Integer targetId);

    boolean isFollowing(Integer followerId, Integer targetId);

    PageResult<UserDTO> listFollowing(Integer followerId, long page, long size);

    PageResult<FeedItemDTO> feed(Integer followerId, long page, long size);
}
