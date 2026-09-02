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

    static final long DOWNSTREAM_TIMEOUT_SECONDS = 3;

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

        CompletableFuture<Bucket> videos = fetch(s.getVideo(), InternalPaths.SEARCH_VIDEOS, kw, clamp(videoLimit));
        CompletableFuture<Bucket> lives = fetch(s.getLive(), InternalPaths.SEARCH_LIVES, kw, clamp(liveLimit));
        CompletableFuture<Bucket> users = fetch(s.getUser(), InternalPaths.SEARCH_USERS, kw, clamp(userLimit));
        CompletableFuture.allOf(videos, lives, users).join();

        Bucket videoBucket = videos.join();
        Bucket liveBucket = lives.join();
        Bucket userBucket = users.join();
        result.setVideos(unchecked(videoBucket.items));
        result.setLiveRooms(unchecked(liveBucket.items));
        result.setUsers(unchecked(userBucket.items));
        if (videoBucket.failed) {
            result.getNotices().add("视频服务超时或不可用，已跳过视频结果（其它分类不受影响）");
        }
        if (liveBucket.failed) {
            result.getNotices().add("直播服务超时或不可用，已跳过直播结果（其它分类不受影响）");
        }
        if (userBucket.failed) {
            result.getNotices().add("用户服务超时或不可用，已跳过用户结果（其它分类不受影响）");
        }
        return result;
    }

    private CompletableFuture<Bucket> fetch(String base, String path, String keyword, long limit) {
        return CompletableFuture.supplyAsync(() -> call(base, path, keyword, limit), executor)
                .orTimeout(DOWNSTREAM_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(ex -> Bucket.down());
    }

    private Bucket call(String base, String path, String keyword, long limit) {
        String uri = ServiceClient.query(ServiceClient.query(path, "keyword", keyword), "limit", String.valueOf(limit));
        CustomResponse resp = serviceClient.get(base, uri);
        if (resp == null || resp.getCode() != 200) {
            return Bucket.down();
        }
        if (resp.getData() instanceof List<?> list) {
            return Bucket.ok(new ArrayList<>(list));
        }
        return Bucket.ok(List.of());
    }

    private static long clamp(long limit) {
        return limit < 1 ? 10 : Math.min(limit, 50);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> unchecked(List<Object> list) {
        return (List<T>) list;
    }

    private record Bucket(List<Object> items, boolean failed) {
        static Bucket ok(List<Object> items) {
            return new Bucket(items, false);
        }

        static Bucket down() {
            return new Bucket(List.of(), true);
        }
    }
}
