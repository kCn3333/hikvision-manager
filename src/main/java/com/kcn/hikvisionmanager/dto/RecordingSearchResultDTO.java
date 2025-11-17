package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RecordingSearchResultDTO {
    private List<RecordingItemDTO> recordings;
    private int currentPage;
    private int pageSize;
    private int totalMatches;
    private boolean hasMore;
    private String searchId;
}