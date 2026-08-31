package com.doinb.message.internal;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.dto.CreateNotificationRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 骨架桩：组员替换为写入 notifications 表。 */
@RestController
public class InternalNotificationController {

    @PostMapping(InternalPaths.NOTIFICATIONS)
    public CustomResponse create(@RequestBody CreateNotificationRequest request) {
        return CustomResponse.ok("骨架桩：已收到创建通知请求，尚未落库", request);
    }
}
