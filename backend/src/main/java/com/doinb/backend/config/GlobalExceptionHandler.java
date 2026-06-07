package com.doinb.backend.config;

import com.doinb.backend.pojo.CustomResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 统一异常响应，便于 Postman 联调 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public CustomResponse handleAuth(AuthenticationException ex) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(401);
        resp.setMessage(ex.getMessage() != null ? ex.getMessage() : "未登录或 Token 无效");
        return resp;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public CustomResponse handleForbidden(AccessDeniedException ex) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(403);
        resp.setMessage(ex.getMessage() != null ? ex.getMessage() : "无权限");
        return resp;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CustomResponse handleBadRequest(IllegalArgumentException ex) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(400);
        resp.setMessage(ex.getMessage());
        return resp;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CustomResponse handleGeneral(Exception ex) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(500);
        resp.setMessage("服务器错误：" + ex.getMessage());
        return resp;
    }
}
