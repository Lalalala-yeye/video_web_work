package com.doinb.gateway.web;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.SearchResultDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;

    public SearchController(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    @GetMapping("/search")
    public CustomResponse search(@RequestParam("keyword") String keyword,
                                 @RequestParam(value = "videoLimit", defaultValue = "10") long videoLimit,
                                 @RequestParam(value = "liveLimit", defaultValue = "10") long liveLimit,
                                 @RequestParam(value = "userLimit", defaultValue = "10") long userLimit) {
        DoinbProperties.Services s = properties.getServices();
        SearchResultDTO result = new SearchResultDTO();
        result.setVideos(asList(call(s.getVideo(), InternalPaths.SEARCH_VIDEOS, keyword, videoLimit)));
        result.setLiveRooms(asList(call(s.getLive(), InternalPaths.SEARCH_LIVES, keyword, liveLimit)));
        result.setUsers(asList(call(s.getUser(), InternalPaths.SEARCH_USERS, keyword, userLimit)));
        return CustomResponse.ok(result);
    }

    private Object call(String base, String path, String keyword, long limit) {
        String uri = ServiceClient.query(ServiceClient.query(path, "keyword", keyword), "limit", String.valueOf(limit));
        CustomResponse resp = serviceClient.get(base, uri);
        return resp.getCode() == 200 ? resp.getData() : List.of();
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> asList(Object data) {
        if (data instanceof List<?> list) {
            return (List<T>) list;
        }
        return List.of();
    }
}
