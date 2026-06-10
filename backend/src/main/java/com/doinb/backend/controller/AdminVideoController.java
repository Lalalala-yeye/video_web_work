package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.VideoDTO;
import com.doinb.backend.service.utils.CurrentUser;
import com.doinb.backend.service.video.AdminVideoService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/video")
public class AdminVideoController {

    private final AdminVideoService adminVideoService;
    private final CurrentUser currentUser;

    public AdminVideoController(AdminVideoService adminVideoService, CurrentUser currentUser) {
        this.adminVideoService = adminVideoService;
        this.currentUser = currentUser;
    }

    @GetMapping("/pending")
    public CustomResponse listPending(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        CustomResponse resp = new CustomResponse();
        resp.setData(adminVideoService.listPending(page, size));
        return resp;
    }

    @GetMapping("/report-review")
    public CustomResponse listReportReview(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        CustomResponse resp = new CustomResponse();
        resp.setData(adminVideoService.listReportReview(page, size));
        return resp;
    }

    @PostMapping("/approve")
    public CustomResponse approve(@RequestParam Integer videoId) {
        return adminVideoService.approve(currentUser.getRole(), videoId);
    }

    @PostMapping("/reject")
    public CustomResponse reject(@RequestParam Integer videoId) {
        return adminVideoService.reject(currentUser.getRole(), videoId);
    }

    @PostMapping("/delete")
    public CustomResponse delete(@RequestParam Integer videoId) {
        return adminVideoService.deleteVideo(currentUser.getRole(), videoId);
    }
}
