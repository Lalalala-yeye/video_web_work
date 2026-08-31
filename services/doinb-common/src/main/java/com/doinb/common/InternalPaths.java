package com.doinb.common;

/** 服务间内部路径。网关不对浏览器转发 /internal/**。 */
public final class InternalPaths {

    public static final String PREFIX = "/internal";

    public static final String SEARCH_USERS = "/internal/search/users";
    public static final String SEARCH_VIDEOS = "/internal/search/videos";
    public static final String SEARCH_LIVES = "/internal/search/lives";

    public static final String USERS = "/internal/users";
    public static final String VIDEOS = "/internal/videos";
    public static final String LIVES = "/internal/lives";
    public static final String NOTIFICATIONS = "/internal/notifications";

    private InternalPaths() {
    }
}
