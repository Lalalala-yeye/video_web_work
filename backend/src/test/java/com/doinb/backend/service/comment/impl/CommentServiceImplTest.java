package com.doinb.backend.service.comment.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.backend.mapper.CommentMapper;
import com.doinb.backend.mapper.LiveRoomMapper;
import com.doinb.backend.mapper.UserMapper;
import com.doinb.backend.mapper.VideoMapper;
import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.CommentDTO;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.entity.Comment;
import com.doinb.backend.pojo.entity.LiveRoom;
import com.doinb.backend.pojo.entity.User;
import com.doinb.backend.pojo.entity.Video;
import com.doinb.backend.service.reaction.ReactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 对应测试报告 C000 / C002 / L040 / L041 */
class CommentServiceImplTest {

    private CommentMapper commentMapper;
    private UserMapper userMapper;
    private VideoMapper videoMapper;
    private LiveRoomMapper liveRoomMapper;
    private ReactionService reactionService;
    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        userMapper = mock(UserMapper.class);
        videoMapper = mock(VideoMapper.class);
        liveRoomMapper = mock(LiveRoomMapper.class);
        reactionService = mock(ReactionService.class);
        service = new CommentServiceImpl(
                commentMapper,
                userMapper,
                videoMapper,
                liveRoomMapper,
                reactionService
        );
    }

    @Test
    void add_whenContentBlank_returns400() {
        CustomResponse resp = service.add(10, 12, 1, "  ");

        assertEquals(400, resp.getCode());
        assertEquals("评论内容不能为空", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenVideoMissing_returns404() {
        when(videoMapper.selectById(99999)).thenReturn(null);

        CustomResponse resp = service.add(10, 99999, 1, "测试评论");

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenLiveNotStarted_returns400() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setIsLive(false);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        CustomResponse resp = service.add(10, 3, 2, "直播弹幕");

        assertEquals(400, resp.getCode());
        assertEquals("直播间未开播，无法发送弹幕", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenVideoExists_returns200() {
        when(videoMapper.selectById(12)).thenReturn(new Video());
        when(userMapper.selectById(10)).thenReturn(new User());

        CustomResponse resp = service.add(10, 12, 1, "测试评论");

        assertEquals(200, resp.getCode());
        assertEquals("评论成功", resp.getMessage());
        verify(commentMapper).insert(any(Comment.class));
    }

    @Test
    void add_whenLiveStarted_returns200() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setIsLive(true);
        when(liveRoomMapper.selectById(3)).thenReturn(room);
        when(userMapper.selectById(10)).thenReturn(new User());

        CustomResponse resp = service.add(10, 3, 2, "直播弹幕");

        assertEquals(200, resp.getCode());
        assertEquals("评论成功", resp.getMessage());
        verify(commentMapper).insert(any(Comment.class));
    }

    @Test
    void add_whenTargetTypeInvalid_returns400() {
        CustomResponse resp = service.add(10, 12, 9, "测试评论");

        assertEquals(400, resp.getCode());
        assertEquals("targetType 无效（1=视频 2=直播间）", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenTargetIdNull_returns400() {
        CustomResponse resp = service.add(10, null, 1, "测试评论");

        assertEquals(400, resp.getCode());
        assertEquals("评论目标无效", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenTargetTypeNull_returns400() {
        CustomResponse resp = service.add(10, 12, null, "测试评论");

        assertEquals(400, resp.getCode());
        assertEquals("评论目标无效", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenContentTooLong_returns400() {
        String longContent = "a".repeat(501);

        CustomResponse resp = service.add(10, 12, 1, longContent);

        assertEquals(400, resp.getCode());
        assertEquals("评论内容不能超过500字", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenLiveRoomMissing_returns404() {
        when(liveRoomMapper.selectById(3)).thenReturn(null);

        CustomResponse resp = service.add(10, 3, 2, "直播弹幕");

        assertEquals(404, resp.getCode());
        assertEquals("直播间不存在", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void listByTarget_whenVideoEmpty_returnsEmptyPage() {
        when(commentMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Comment> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<CommentDTO> result = service.listByTarget(12, 1, 1, 10, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void listByTarget_whenVideoHasComments_returnsDTOs() {
        Comment comment = new Comment();
        comment.setId(1);
        comment.setUserId(10);
        comment.setTargetId(12);
        comment.setTargetType(1);
        comment.setContent("hello");
        comment.setCreateTime(LocalDateTime.now());
        when(commentMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<Comment> page = invocation.getArgument(0);
            page.setRecords(List.of(comment));
            page.setTotal(1);
            return page;
        });
        User user = new User();
        user.setId(10);
        user.setNickname("评论者");
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user));
        when(reactionService.getCommentSummaries(any(), any())).thenReturn(Map.of());

        PageResult<CommentDTO> result = service.listByTarget(12, 1, 1, 10, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("评论者", result.getRecords().get(0).getUserNickname());
    }

    @Test
    void listByTarget_whenLiveRoomMissing_returnsEmptyPage() {
        when(liveRoomMapper.selectById(3)).thenReturn(null);

        PageResult<CommentDTO> result = service.listByTarget(3, 2, 1, 10, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void listByTarget_whenLiveSessionMissing_returnsEmptyPage() {
        LiveRoom room = new LiveRoom();
        room.setId(3);
        room.setIsLive(true);
        room.setSessionStart(null);
        when(liveRoomMapper.selectById(3)).thenReturn(room);

        PageResult<CommentDTO> result = service.listByTarget(3, 2, 1, 10, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }
}
