package com.doinb.backend.controller;

import com.doinb.backend.pojo.CustomResponse;
import com.doinb.backend.pojo.dto.SearchResultDTO;
import com.doinb.backend.service.search.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 搜索接口 */
@RestController
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "videoLimit", defaultValue = "10") long videoLimit,
                                 @RequestParam(value = "liveLimit", defaultValue = "10") long liveLimit,
                                 @RequestParam(value = "userLimit", defaultValue = "10") long userLimit) {
        SearchResultDTO result = searchService.search(keyword, videoLimit, liveLimit, userLimit);
        CustomResponse resp = new CustomResponse();
        resp.setData(result);
        return resp;
    }
}
