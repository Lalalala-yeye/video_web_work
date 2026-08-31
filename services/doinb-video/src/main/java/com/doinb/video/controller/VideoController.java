package com.doinb.video.controller;

import com.doinb.common.CustomResponse;
import com.doinb.common.PageResult;
import com.doinb.common.dto.VideoDTO;
import com.doinb.video.pojo.dto.PlayHistoryDTO;
import com.doinb.video.service.VideoService;
import com.doinb.video.web.Viewer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @GetMapping("/video/list")
    public CustomResponse list(@RequestParam(value = "page", defaultValue = "1") long page,
                               @RequestParam(value = "size", defaultValue = "12") long size) {
        return CustomResponse.ok(videoService.listPublished(page, size));
    }

    @GetMapping("/video/getone")
    public CustomResponse getOne(@RequestParam("id") Integer id, HttpServletRequest request) {
        return videoService.getOne(id, Viewer.userId(request), Viewer.role(request));
    }

    @PostMapping("/video/history/progress")
    public CustomResponse saveProgress(@RequestParam("videoId") Integer videoId,
                                       @RequestParam("progress") Integer progress,
                                       HttpServletRequest request) {
        return videoService.saveProgress(Viewer.requireUserId(request), videoId, progress);
    }

    @GetMapping("/video/history/list")
    public CustomResponse historyList(@RequestParam(value = "page", defaultValue = "1") long page,
                                      @RequestParam(value = "size", defaultValue = "12") long size,
                                      HttpServletRequest request) {
        PageResult<PlayHistoryDTO> result = videoService.listHistory(Viewer.requireUserId(request), page, size);
        return CustomResponse.ok(result);
    }

    @PostMapping("/video/upload")
    public CustomResponse upload(@RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "visibility", defaultValue = "public") String visibility,
                                 @RequestParam(value = "cover", required = false) MultipartFile cover,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 HttpServletRequest request) {
        return videoService.upload(Viewer.requireUserId(request), Viewer.requireRole(request),
                title, description, visibility, cover, file);
    }

    @GetMapping("/video/my/list")
    public CustomResponse myList(@RequestParam(value = "page", defaultValue = "1") long page,
                                 @RequestParam(value = "size", defaultValue = "12") long size,
                                 HttpServletRequest request) {
        PageResult<VideoDTO> result = videoService.listMyVideos(Viewer.requireUserId(request), page, size);
        return CustomResponse.ok(result);
    }

    @PostMapping("/video/update")
    public CustomResponse update(@RequestParam("id") Integer id,
                                 @RequestParam("title") String title,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "visibility", defaultValue = "public") String visibility,
                                 @RequestParam(value = "cover", required = false) MultipartFile cover,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 HttpServletRequest request) {
        return videoService.updateVideo(Viewer.requireUserId(request), Viewer.requireRole(request),
                id, title, description, visibility, cover, file);
    }

    @GetMapping("/video/my/getone")
    public CustomResponse myGetOne(@RequestParam("id") Integer id, HttpServletRequest request) {
        return videoService.getMyVideo(Viewer.requireUserId(request), Viewer.requireRole(request), id);
    }

    @PostMapping("/video/visibility")
    public CustomResponse setVisibility(@RequestParam("id") Integer id,
                                        @RequestParam("visibility") String visibility,
                                        HttpServletRequest request) {
        return videoService.setVisibility(Viewer.requireUserId(request), Viewer.requireRole(request), id, visibility);
    }

    @PostMapping("/video/report")
    public CustomResponse report(@RequestParam("id") Integer id,
                                 @RequestParam(value = "reason", required = false) String reason,
                                 HttpServletRequest request) {
        return videoService.reportVideo(Viewer.requireUserId(request), id, reason);
    }

    @PostMapping("/video/delete")
    public CustomResponse delete(@RequestParam("id") Integer id, HttpServletRequest request) {
        return videoService.deleteVideo(Viewer.requireUserId(request), Viewer.requireRole(request), id);
    }
}
