package com.doinb.backend.pojo;

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
}
