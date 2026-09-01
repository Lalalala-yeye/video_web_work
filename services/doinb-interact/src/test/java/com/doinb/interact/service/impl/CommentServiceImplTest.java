package com.doinb.interact.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.LiveRoomDTO;
import com.doinb.common.dto.UserDTO;
import com.doinb.common.dto.VideoDTO;
import com.doinb.interact.client.LiveDirectory;
import com.doinb.interact.client.UserDirectory;
import com.doinb.interact.client.VideoDirectory;
import com.doinb.interact.mapper.CommentMapper;
import com.doinb.interact.pojo.dto.CommentDTO;
import com.doinb.interact.pojo.entity.Comment;
import com.doinb.interact.service.ReactionService;
import com.doinb.interact.support.MybatisPlusLambdaCacheExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(MybatisPlusLambdaCacheExtension.class)
class CommentServiceImplTest {

    private CommentMapper commentMapper;
    private ReactionService reactionService;
    private VideoDirectory videoDirectory;
    private LiveDirectory liveDirectory;
    private UserDirectory userDirectory;
    private CommentServiceImpl service;

    @BeforeEach
    void setUp() {
        commentMapper = mock(CommentMapper.class);
        reactionService = mock(ReactionService.class);
        videoDirectory = mock(VideoDirectory.class);
        liveDirectory = mock(LiveDirectory.class);
        userDirectory = mock(UserDirectory.class);
        service = new CommentServiceImpl(commentMapper, reactionService, videoDirectory, liveDirectory, userDirectory);
    }

    @Test
    void add_whenContentBlank_returns400() {
        CustomResponse resp = service.add(10, 12, 1, "  ");

        assertEquals(400, resp.getCode());
        assertEquals("评论内容不能为空", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenContentTooLong_returns400() {
        CustomResponse resp = service.add(10, 12, 1, "a".repeat(501));

        assertEquals(400, resp.getCode());
        assertEquals("评论内容不能超过500字", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenTargetIdNull_returns400() {
        CustomResponse resp = service.add(10, null, 1, "测试评论");

        assertEquals(400, resp.getCode());
        assertEquals("评论目标无效", resp.getMessage());
    }

    @Test
    void add_whenTargetTypeInvalid_returns400() {
        CustomResponse resp = service.add(10, 12, 9, "测试评论");

        assertEquals(400, resp.getCode());
        assertEquals("targetType 无效（1=视频 2=直播间）", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenVideoMissing_returns404() {
        when(videoDirectory.findById(99999)).thenReturn(null);

        CustomResponse resp = service.add(10, 99999, 1, "测试评论");

        assertEquals(404, resp.getCode());
        assertEquals("视频不存在", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenLiveRoomMissing_returns404() {
        when(liveDirectory.findById(3)).thenReturn(null);

        CustomResponse resp = service.add(10, 3, 2, "直播弹幕");

        assertEquals(404, resp.getCode());
        assertEquals("直播间不存在", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenLiveNotStarted_returns400() {
        LiveRoomDTO room = new LiveRoomDTO();
        room.setId(3);
        room.setIsLive(false);
        when(liveDirectory.findById(3)).thenReturn(room);

        CustomResponse resp = service.add(10, 3, 2, "直播弹幕");

        assertEquals(400, resp.getCode());
        assertEquals("直播间未开播，无法发送弹幕", resp.getMessage());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void add_whenVideoExists_returns200() {
        when(videoDirectory.findById(12)).thenReturn(new VideoDTO());
        UserDTO user = new UserDTO();
        user.setId(10);
        user.setNickname("评论者");
        when(userDirectory.findById(10)).thenReturn(user);

        CustomResponse resp = service.add(10, 12, 1, "测试评论");

        assertEquals(200, resp.getCode());
        assertEquals("评论成功", resp.getMessage());
        verify(commentMapper).insert(any(Comment.class));
        CommentDTO dto = (CommentDTO) resp.getData();
        assertEquals("评论者", dto.getUserNickname());
    }

    @Test
    void add_whenLiveStarted_returns200() {
        LiveRoomDTO room = new LiveRoomDTO();
        room.setId(3);
        room.setIsLive(true);
        when(liveDirectory.findById(3)).thenReturn(room);

        CustomResponse resp = service.add(10, 3, 2, "直播弹幕");

        assertEquals(200, resp.getCode());
        assertEquals("评论成功", resp.getMessage());
        verify(commentMapper).insert(any(Comment.class));
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
        UserDTO user = new UserDTO();
        user.setId(10);
        user.setNickname("评论者");
        when(userDirectory.findByIds(any())).thenReturn(Map.of(10, user));
        when(reactionService.getCommentSummaries(any(), any())).thenReturn(Map.of());

        PageResult<CommentDTO> result = service.listByTarget(12, 1, 1, 10, 10);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals("评论者", result.getRecords().get(0).getUserNickname());
    }

    @Test
    void listByTarget_whenLiveRoomMissing_returnsEmptyPage() {
        when(liveDirectory.findById(3)).thenReturn(null);

        PageResult<CommentDTO> result = service.listByTarget(3, 2, 1, 10, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @Test
    void listByTarget_whenLiveNotStarted_returnsEmptyPage() {
        LiveRoomDTO room = new LiveRoomDTO();
        room.setId(3);
        room.setIsLive(false);
        when(liveDirectory.findById(3)).thenReturn(room);

        PageResult<CommentDTO> result = service.listByTarget(3, 2, 1, 10, 10);

        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }
}
