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

@RestController
public class VideoController {

    private final VideoService videoService;
    private final CurrentUser currentUser;

    public VideoController(VideoService videoService, CurrentUser currentUser) {
        this.videoService = videoService;
        this.currentUser = currentUser;
    }

    @GetMapping("/video/list")
    public CustomResponse list(@RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<VideoDTO> result = videoService.listPublished(page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    @GetMapping("/video/getone")
    public CustomResponse getOne(@RequestParam("id") Integer id) {
        return videoService.getOne(id, safeViewerId(), safeViewerRole());
    }

    @PostMapping("/video/history/progress")
    public CustomResponse saveProgress(@RequestParam("videoId") Integer videoId,
                                       @RequestParam("progress") Integer progress) {
        return videoService.saveProgress(currentUser.getUserId(), videoId, progress);
    }

    @GetMapping("/video/history/list")
    public CustomResponse historyList(@RequestParam(value = "page", defaultValue = "1") long page,
                                      @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<PlayHistoryDTO> result = videoService.listHistory(currentUser.getUserId(), page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    @PostMapping("/video/upload")
    public CustomResponse upload(@RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "visibility", defaultValue = "public") String visibility,
                                 @RequestParam(value = "cover", required = false) MultipartFile cover,
                                 @RequestParam("file") MultipartFile file) {
        UserDetailsImpl loginUser = currentLoginUser();
        return videoService.upload(loginUser.getUser().getId(), loginUser.getUser().getRole(),
                title, description, visibility, cover, file);
    }

    @GetMapping("/video/my/list")
    public CustomResponse myList(@RequestParam(value = "page", defaultValue = "1") long page,
                                 @RequestParam(value = "size", defaultValue = "12") long size) {
        PageResult<VideoDTO> result = videoService.listMyVideos(currentUser.getUserId(), page, size);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }

    @PostMapping("/video/update")
    public CustomResponse update(@RequestParam("id") Integer id,
                                 @RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "visibility", defaultValue = "public") String visibility,
                                 @RequestParam(value = "cover", required = false) MultipartFile cover,
                                 @RequestParam(value = "file", required = false) MultipartFile file) {
        UserDetailsImpl loginUser = currentLoginUser();
        return videoService.updateVideo(loginUser.getUser().getId(), loginUser.getUser().getRole(),
                id, title, description, visibility, cover, file);
    }

    @GetMapping("/video/my/getone")
    public CustomResponse myGetOne(@RequestParam("id") Integer id) {
        UserDetailsImpl loginUser = currentLoginUser();
        return videoService.getMyVideo(loginUser.getUser().getId(), loginUser.getUser().getRole(), id);
    }

    @PostMapping("/video/visibility")
    public CustomResponse setVisibility(@RequestParam("id") Integer id,
                                        @RequestParam("visibility") String visibility) {
        UserDetailsImpl loginUser = currentLoginUser();
        return videoService.setVisibility(loginUser.getUser().getId(), loginUser.getUser().getRole(),
                id, visibility);
    }

    @PostMapping("/video/report")
    public CustomResponse report(@RequestParam("id") Integer id,
                                 @RequestParam(value = "reason", required = false) String reason) {
        return videoService.reportVideo(currentUser.getUserId(), id, reason);
    }

    @PostMapping("/video/delete")
    public CustomResponse delete(@RequestParam("id") Integer id) {
        UserDetailsImpl loginUser = currentLoginUser();
        return videoService.deleteVideo(loginUser.getUser().getId(), loginUser.getUser().getRole(), id);
    }

    private UserDetailsImpl currentLoginUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Integer safeViewerId() {
        try {
            return currentUser.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Integer safeViewerRole() {
        try {
            return currentUser.getRole();
        } catch (Exception e) {
            return null;
        }
    }
}
