package com.doinb.backend.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 分页列表的统一返回结构 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private long total;
    private long page;
    private long size;
    private List<T> records;
}
