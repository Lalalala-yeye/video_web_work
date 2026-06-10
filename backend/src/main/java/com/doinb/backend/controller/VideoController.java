package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.PageResult;
import com.doinb.backend.pojo.dto.PlayHistoryDTO;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.service.users.impl.UserDetailsImpl;
import com.doinb.backend.service.utils.CurrentUser;
import com.doinb.backend.service.video.VideoService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 视频接口：列表、播放、历史、上传。
 */
@RestController
public class VideoController {

    private final VideoService videoService;
    private final CurrentUser currentUser;

    public VideoController(VideoService videoService, CurrentUser currentUser) {
        this.videoService = videoService;
        this.currentUser = currentUser;
    }

    /** 已发布视频列表（游客可访问） */
    @GetMapping("/video/list")
    public CustomResponse list(@RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<VideoDTO> result = videoService.listPublished(page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    /** 视频详情与播放地址（游客可访问） */
    @GetMapping("/video/getone")
    public CustomResponse getOne(@RequestParam("id") Integer id) {
        return videoService.getOne(id, safeViewerId());
    }

    /** 保存播放进度（需登录） */
    @PostMapping("/video/history/progress")
    public CustomResponse saveProgress(@RequestParam("videoId") Integer videoId,
                                       @RequestParam("progress") Integer progress) {
        Integer userId = currentUser.getUserId();
        return videoService.saveProgress(userId, videoId, progress);
    }

    /** 我的播放历史（需登录） */
    @GetMapping("/video/history/list")
    public CustomResponse historyList(@RequestParam(value = "page", defaultValue = "1") long page,
                                      @RequestParam(value = "size", defaultValue = "12") long size) {
        Integer userId = currentUser.getUserId();
        PageResult<PlayHistoryDTO> result = videoService.listHistory(userId, page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    /** 上传视频（需登录） */
    @PostMapping("/video/upload")
    public CustomResponse upload(@RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "cover", required = false) MultipartFile cover,
                                 @RequestParam("file") MultipartFile file) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        Integer userId = loginUser.getUser().getId();
        Integer role = loginUser.getUser().getRole();
        return videoService.upload(userId, role, title, description, cover, file);
    }

    /** 我上传的视频（含各状态，需登录） */
    @GetMapping("/video/my/list")
    public CustomResponse myList(@RequestParam(value = "page", defaultValue = "1") long page,
                                 @RequestParam(value = "size", defaultValue = "12") long size) {
        Integer userId = currentUser.getUserId();
        PageResult<VideoDTO> result = videoService.listMyVideos(userId, page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    /** 编辑视频（作者或管理员，可选更换封面/视频） */
    @PostMapping("/video/update")
    public CustomResponse update(@RequestParam("id") Integer id,
                                 @RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "cover", required = false) MultipartFile cover,
                                 @RequestParam(value = "file", required = false) MultipartFile file) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return videoService.updateVideo(loginUser.getUser().getId(), loginUser.getUser().getRole(),
                id, title, description, cover, file);
    }

    /** 获取本人可编辑的视频详情 */
    @GetMapping("/video/my/getone")
    public CustomResponse myGetOne(@RequestParam("id") Integer id) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return videoService.getMyVideo(loginUser.getUser().getId(), loginUser.getUser().getRole(), id);
    }

    /** 修改视频状态（作者或管理员） */
    @PostMapping("/video/status")
    public CustomResponse updateStatus(@RequestParam("id") Integer id,
                                       @RequestParam("status") Integer status) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return videoService.updateStatus(loginUser.getUser().getId(), loginUser.getUser().getRole(),
                id, status);
    }

    /** 删除视频（作者或管理员） */
    @PostMapping("/video/delete")
    public CustomResponse delete(@RequestParam("id") Integer id) {
        UserDetailsImpl loginUser = (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return videoService.deleteVideo(loginUser.getUser().getId(), loginUser.getUser().getRole(), id);
    }

    private Integer safeViewerId() {
        try {
            return currentUser.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
