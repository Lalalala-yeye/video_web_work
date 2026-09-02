package com.doinb.common.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 服务间 HTTP：强制 HTTP/1.1 复用连接，并给发送线程固定池。
 * JDK 默认客户端在 Docker 里容易每请求新建连接，尾延迟会被拉到秒级。
 */
public final class InternalHttp {

    private InternalHttp() {
    }

    public static HttpClient jdkClient(int threads) {
        AtomicInteger n = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "doinb-http-" + n.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(3))
                .executor(Executors.newFixedThreadPool(Math.max(8, threads), factory))
                .build();
    }
}
