package com.doinb.gateway.search;

import com.doinb.common.dto.SearchResultDTO;

public interface SearchService {

    SearchResultDTO search(String keyword, long videoLimit, long liveLimit, long userLimit);
}
