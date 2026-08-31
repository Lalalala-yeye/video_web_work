package com.doinb.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CustomResponse {
    private int code = 200;
    private String message = "OK";
    private Object data;

    public static CustomResponse ok() {
        return new CustomResponse();
    }

    public static CustomResponse ok(Object data) {
        CustomResponse resp = new CustomResponse();
        resp.setData(data);
        return resp;
    }

    public static CustomResponse ok(String message, Object data) {
        CustomResponse resp = new CustomResponse();
        resp.setMessage(message);
        resp.setData(data);
        return resp;
    }

    public static CustomResponse fail(int code, String message) {
        CustomResponse resp = new CustomResponse();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }
}
