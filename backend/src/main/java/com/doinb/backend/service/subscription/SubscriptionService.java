package com.doinb.backend.service.subscription;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.FeedItemDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.UserDTO;

/** 订阅/关注业务 */
public interface SubscriptionService {

    CustomResponse follow(Integer followerId, Integer targetId);

    CustomResponse unfollow(Integer followerId, Integer targetId);

    PageResult<UserDTO> listFollowing(Integer followerId, long page, long size);

    PageResult<FeedItemDTO> feed(Integer followerId, long page, long size);
}
