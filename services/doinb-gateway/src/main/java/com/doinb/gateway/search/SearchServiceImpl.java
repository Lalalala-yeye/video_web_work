package com.doinb.gateway.search;

import com.doinb.common.CustomResponse;
import com.doinb.common.InternalPaths;
import com.doinb.common.client.ServiceClient;
import com.doinb.common.config.DoinbProperties;
import com.doinb.common.dto.SearchResultDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class SearchServiceImpl implements SearchService {

    private static final long DOWNSTREAM_TIMEOUT_SECONDS = 3;

    private final ServiceClient serviceClient;
    private final DoinbProperties properties;
    private final ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "gateway-search");
        t.setDaemon(true);
        return t;
    });

    public SearchServiceImpl(ServiceClient serviceClient, DoinbProperties properties) {
        this.serviceClient = serviceClient;
        this.properties = properties;
    }

    @Override
    public SearchResultDTO search(String keyword, long videoLimit, long liveLimit, long userLimit) {
        SearchResultDTO result = new SearchResultDTO();
        if (!StringUtils.hasText(keyword)) {
            return result;
        }
        String kw = keyword.trim();
        DoinbProperties.Services s = properties.getServices();

        CompletableFuture<List<Object>> videos = fetch(s.getVideo(), InternalPaths.SEARCH_VIDEOS, kw, clamp(videoLimit));
        CompletableFuture<List<Object>> lives = fetch(s.getLive(), InternalPaths.SEARCH_LIVES, kw, clamp(liveLimit));
        CompletableFuture<List<Object>> users = fetch(s.getUser(), InternalPaths.SEARCH_USERS, kw, clamp(userLimit));
        CompletableFuture.allOf(videos, lives, users).join();

        result.setVideos(unchecked(videos.join()));
        result.setLiveRooms(unchecked(lives.join()));
        result.setUsers(unchecked(users.join()));
        return result;
    }

    private CompletableFuture<List<Object>> fetch(String base, String path, String keyword, long limit) {
        return CompletableFuture.supplyAsync(() -> call(base, path, keyword, limit), executor)
                .orTimeout(DOWNSTREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> List.of());
    }

    private List<Object> call(String base, String path, String keyword, long limit) {
        String uri = ServiceClient.query(ServiceClient.query(path, "keyword", keyword), "limit", String.valueOf(limit));
        CustomResponse resp = serviceClient.get(base, uri);
        if (resp == null || resp.getCode() != 200) {
            return List.of();
        }
        if (resp.getData() instanceof List<?> list) {
            return new ArrayList<>(list);
        }
        return List.of();
    }

    private static long clamp(long limit) {
        return limit < 1 ? 10 : Math.min(limit, 50);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> unchecked(List<Object> list) {
        return (List<T>) list;
    }
}
