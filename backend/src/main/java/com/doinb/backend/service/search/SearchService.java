package com.doinb.backend.service.search;

import com.doinb.backend.pojo.dto.SearchResultDTO;

/** 搜索业务 */
public interface SearchService {

    SearchResultDTO search(String keyword, long videoLimit, long liveLimit, long userLimit);
}
