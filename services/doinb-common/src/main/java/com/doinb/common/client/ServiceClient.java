package com.doinb.common.client;

import com.doinb.common.CustomResponse;
import com.doinb.common.GatewayHeaders;
import com.doinb.common.config.DoinbProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** 服务间同步 HTTP。一律带内部令牌，禁止走浏览器那套 JWT。 */
@Component
public class ServiceClient {

    private final RestClient restClient;
    private final DoinbProperties properties;

    public ServiceClient(DoinbProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(InternalHttp.jdkClient(16));
        factory.setReadTimeout(java.time.Duration.ofSeconds(8));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public CustomResponse get(String baseUrl, String pathAndQuery) {
        try {
            CustomResponse body = restClient.get()
                    .uri(URI.create(join(baseUrl, pathAndQuery)))
                    .header(GatewayHeaders.INTERNAL_TOKEN, properties.getInternalToken())
                    .retrieve()
                    .body(CustomResponse.class);
            return body != null ? body : CustomResponse.fail(502, "下游无响应");
        } catch (RestClientException ex) {
            return CustomResponse.fail(502, "调用下游失败：" + ex.getMessage());
        }
    }

    public CustomResponse post(String baseUrl, String path, Object jsonBody) {
        try {
            CustomResponse body = restClient.post()
                    .uri(URI.create(join(baseUrl, path)))
                    .header(GatewayHeaders.INTERNAL_TOKEN, properties.getInternalToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonBody)
                    .retrieve()
                    .body(CustomResponse.class);
            return body != null ? body : CustomResponse.fail(502, "下游无响应");
        } catch (RestClientException ex) {
            return CustomResponse.fail(502, "调用下游失败：" + ex.getMessage());
        }
    }

    public static String query(String path, String name, String value) {
        String encoded = java.net.URLEncoder.encode(Objects.requireNonNullElse(value, ""), StandardCharsets.UTF_8);
        return path + (path.contains("?") ? "&" : "?") + name + "=" + encoded;
    }

    private static String join(String baseUrl, String path) {
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        return baseUrl + path;
    }
}
