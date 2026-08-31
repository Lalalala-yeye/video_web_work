package com.doinb.video.controller;

import com.doinb.common.CustomResponse;
import com.doinb.video.service.AdminVideoService;
import com.doinb.video.web.Viewer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/video")
public class AdminVideoController {

    private final AdminVideoService adminVideoService;

    public AdminVideoController(AdminVideoService adminVideoService) {
        this.adminVideoService = adminVideoService;
    }

    @GetMapping("/pending")
    public CustomResponse listPending(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      HttpServletRequest request) {
        CustomResponse resp = new CustomResponse();
        try {
            resp.setData(adminVideoService.listPending(Viewer.requireRole(request), page, size));
        } catch (SecurityException e) {
            resp.setCode(403);
            resp.setMessage(e.getMessage());
        }
        return resp;
    }

    @GetMapping("/report-review")
    public CustomResponse listReportReview(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           HttpServletRequest request) {
        CustomResponse resp = new CustomResponse();
        try {
            resp.setData(adminVideoService.listReportReview(Viewer.requireRole(request), page, size));
        } catch (SecurityException e) {
            resp.setCode(403);
            resp.setMessage(e.getMessage());
        }
        return resp;
    }

    @GetMapping("/getone")
    public CustomResponse getOne(@RequestParam Integer id, HttpServletRequest request) {
        return adminVideoService.getVideoForPreview(Viewer.requireRole(request), id);
    }

    @GetMapping("/reports")
    public CustomResponse listReports(@RequestParam Integer videoId, HttpServletRequest request) {
        CustomResponse resp = new CustomResponse();
        try {
            resp.setData(adminVideoService.listReports(Viewer.requireRole(request), videoId));
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
    public CustomResponse approve(@RequestParam Integer videoId, HttpServletRequest request) {
        return adminVideoService.approve(Viewer.requireRole(request), Viewer.requireUserId(request), videoId);
    }

    @PostMapping("/reject")
    public CustomResponse reject(@RequestParam Integer videoId, HttpServletRequest request) {
        return adminVideoService.reject(Viewer.requireRole(request), videoId);
    }

    @PostMapping("/delete")
    public CustomResponse delete(@RequestParam Integer videoId, HttpServletRequest request) {
        return adminVideoService.deleteVideo(Viewer.requireRole(request), videoId);
    }
}
