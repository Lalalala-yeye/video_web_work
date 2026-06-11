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
        try {
            resp.setData(adminVideoService.listPending(currentUser.getRole(), page, size));
        } catch (SecurityException e) {
            resp.setCode(403);
            resp.setMessage(e.getMessage());
        }
        return resp;
    }

    @GetMapping("/report-review")
    public CustomResponse listReportReview(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        CustomResponse resp = new CustomResponse();
        try {
            resp.setData(adminVideoService.listReportReview(currentUser.getRole(), page, size));
        } catch (SecurityException e) {
            resp.setCode(403);
            resp.setMessage(e.getMessage());
        }
        return resp;
    }

    @GetMapping("/getone")
    public CustomResponse getOne(@RequestParam Integer id) {
        return adminVideoService.getVideoForPreview(currentUser.getRole(), id);
    }

    @GetMapping("/reports")
    public CustomResponse listReports(@RequestParam Integer videoId) {
        CustomResponse resp = new CustomResponse();
        try {
            resp.setData(adminVideoService.listReports(currentUser.getRole(), videoId));
        } catch (SecurityException e) {
            resp.setCode(403);
            resp.setMessage(e.getMessage());
        } catch (IllegalArgumentException e) {
            resp.setCode(400);
            resp.setMessage(e.getMessage());
        }
        return resp;
    }

    @PostMapping("/approve")
    public CustomResponse approve(@RequestParam Integer videoId) {
        return adminVideoService.approve(currentUser.getRole(), currentUser.getUserId(), videoId);
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
