package com.doinb.video.client;

import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.CreateNotificationRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageNotifierTest {

    @Test
    void notifyVideoApproved_postsType4WithLinkPath() {
        ServiceClient client = mock(ServiceClient.class);
        DoinbProperties properties = new DoinbProperties();
        properties.getServices().setMessage("http://127.0.0.1:8086");
        MessageNotifier notifier = new MessageNotifier(client, properties);

        notifier.notifyVideoApproved(11, 10, 12, "春季赛集锦");

        ArgumentCaptor<CreateNotificationRequest> captor = ArgumentCaptor.forClass(CreateNotificationRequest.class);
        verify(client).post(eq("http://127.0.0.1:8086"), eq(InternalPaths.NOTIFICATIONS), captor.capture());
        CreateNotificationRequest n = captor.getValue();
        assertEquals(10, n.getUserId());
        assertEquals(4, n.getType());
        assertEquals(11, n.getActorId());
        assertEquals(12, n.getRefId());
        assertEquals("/video/12", n.getLinkPath());
        assertEquals("你的视频《春季赛集锦》已通过审核并公开发布", n.getPreview());
    }
}
