package com.doinb.gateway.web;

import com.doinb.common.CustomResponse;
import com.doinb.gateway.search.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        return CustomResponse.ok(searchService.search(keyword, videoLimit, liveLimit, userLimit));
    }
}
