package com.doinb.common;

/** 网关注入、服务间调用使用的请求头。客户端自带的同名头一律覆盖。 */
public final class GatewayHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String INTERNAL_TOKEN = "X-Internal-Token";

    /** JWT claim role：普通用户 */
    public static final String ROLE_USER = "user";
    /** JWT claim role：管理员 */
    public static final String ROLE_ADMIN = "admin";

    private GatewayHeaders() {
    }
}
