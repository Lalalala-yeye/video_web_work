package com.doinb.backend.service.notification.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.NotificationMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.NotificationDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.Notification;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 N010 / N021 */
class NotificationServiceImplTest {

    private NotificationMapper notificationMapper;
    private VideoMapper videoMapper;
    private UserMapper userMapper;
    private CommentMapper commentMapper;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationMapper = mock(NotificationMapper.class);
        videoMapper = mock(VideoMapper.class);
        userMapper = mock(UserMapper.class);
        commentMapper = mock(CommentMapper.class);
        service = new NotificationServiceImpl(
                notificationMapper,
                userMapper,
                videoMapper,
                commentMapper
        );
    }

    @Test
    void countUnread_returnsMapperCount() {
        when(notificationMapper.selectCount(any(Wrapper.class))).thenReturn(4L);

        assertEquals(4L, service.countUnread(10));
    }

    @Test
    void markRead_whenMissing_returns404() {
        when(notificationMapper.selectById(99)).thenReturn(null);

        CustomResponse resp = service.markRead(10, 99);

        assertEquals(404, resp.getCode());
        assertEquals("通知不存在", resp.getMessage());
    }

    @Test
    void markRead_whenAll_returns200() {
        CustomResponse resp = service.markRead(10, null);

        assertEquals(200, resp.getCode());
        assertEquals("已全部标为已读", resp.getMessage());
        verify(notificationMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void notifyVideoLike_whenSelfLike_doesNotInsert() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        when(videoMapper.selectById(12)).thenReturn(video);

        service.notifyVideoLike(10, 12);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void notifyVideoLike_whenOtherUser_inserts() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setTitle("132");
        when(videoMapper.selectById(12)).thenReturn(video);

        service.notifyVideoLike(5, 12);

        verify(notificationMapper).insert(any(Notification.class));
    }

    @Test
    void markRead_whenSingleExists_returns200() {
        Notification n = new Notification();
        n.setId(5);
        n.setUserId(10);
        when(notificationMapper.selectById(5)).thenReturn(n);

        CustomResponse resp = service.markRead(10, 5);

        assertEquals(200, resp.getCode());
        assertEquals("已标为已读", resp.getMessage());
        verify(notificationMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void markRead_whenSingleNotOwner_returns404() {
        Notification n = new Notification();
        n.setId(5);
        n.setUserId(99);
        when(notificationMapper.selectById(5)).thenReturn(n);

        CustomResponse resp = service.markRead(10, 5);

        assertEquals(404, resp.getCode());
        assertEquals("通知不存在", resp.getMessage());
    }

    @Test
    void notifyCommentLike_whenSelf_doesNotInsert() {
        Comment comment = new Comment();
        comment.setId(46);
        comment.setUserId(10);
        when(commentMapper.selectById(46)).thenReturn(comment);

        service.notifyCommentLike(10, 46);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void notifyCommentLike_whenOtherUser_inserts() {
        Comment comment = new Comment();
        comment.setId(46);
        comment.setUserId(10);
        when(commentMapper.selectById(46)).thenReturn(comment);

        service.notifyCommentLike(5, 46);

        verify(notificationMapper).insert(any(Notification.class));
    }

    @Test
    void notifyMessage_whenSelf_doesNotInsert() {
        service.notifyMessage(10, 10, 2, "hi");

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void notifyMessage_whenOtherUser_inserts() {
        service.notifyMessage(10, 11, 2, "hi");

        verify(notificationMapper).insert(any(Notification.class));
    }

    @Test
    void notifyVideoApproved_whenVideoMissing_doesNotInsert() {
        when(videoMapper.selectById(12)).thenReturn(null);

        service.notifyVideoApproved(11, 12);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void notifyVideoApproved_whenAuthorNull_doesNotInsert() {
        Video video = new Video();
        video.setId(12);
        when(videoMapper.selectById(12)).thenReturn(video);

        service.notifyVideoApproved(11, 12);

        verify(notificationMapper, never()).insert(any(Notification.class));
    }

    @Test
    void notifyVideoApproved_whenOtherUser_inserts() {
        Video video = new Video();
        video.setId(12);
        video.setAuthorId(10);
        video.setTitle("标题");
        when(videoMapper.selectById(12)).thenReturn(video);

        service.notifyVideoApproved(11, 12);

        verify(notificationMapper).insert(any(Notification.class));
    }

    @Test
    void list_whenEmpty_returnsEmptyPage() {
        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Notification> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<NotificationDTO> result = service.list(10, 1, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void list_whenHasRows_buildsLinkPaths() {
        Notification likeVideo = new Notification();
        likeVideo.setId(1);
        likeVideo.setType(1);
        likeVideo.setActorId(5);
        likeVideo.setRefId(12);
        likeVideo.setPreview("赞了你的视频");
        likeVideo.setIsRead(false);

        Notification commentLike = new Notification();
        commentLike.setId(2);
        commentLike.setType(2);
        commentLike.setActorId(5);
        commentLike.setRefId(46);
        commentLike.setPreview("赞了你的评论");
        commentLike.setIsRead(false);

        Notification message = new Notification();
        message.setId(3);
        message.setType(3);
        message.setActorId(6);
        message.setRefId(2);
        message.setPreview("私信");
        message.setIsRead(false);

        Notification approved = new Notification();
        approved.setId(4);
        approved.setType(4);
        approved.setActorId(7);
        approved.setRefId(13);
        approved.setPreview("通过审核");
        approved.setIsRead(true);

        when(notificationMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Notification> page = invocation.getArgument(0);
            page.setRecords(List.of(likeVideo, commentLike, message, approved));
            page.setTotal(4);
            return page;
        });
        User actor = new User();
        actor.setId(5);
        actor.setNickname("演员");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(actor));
        Comment comment = new Comment();
        comment.setId(46);
        comment.setTargetType(1);
        comment.setTargetId(99);
        when(commentMapper.selectBatchIds(any())).thenReturn(List.of(comment));

        PageResult<NotificationDTO> result = service.list(10, 1, 10);

        assertEquals(4, result.getRecords().size());
        assertEquals("/video/12", result.getRecords().get(0).getLinkPath());
        assertEquals("演员", result.getRecords().get(0).getActorNickname());
        assertEquals("/video/99", result.getRecords().get(1).getLinkPath());
        assertEquals("/messages/2", result.getRecords().get(2).getLinkPath());
        assertEquals("doinb", result.getRecords().get(3).getActorNickname());
        assertEquals("/video/13", result.getRecords().get(3).getLinkPath());
    }
}
