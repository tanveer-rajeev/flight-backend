package com.aerionsoft.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarkupPlanMultiBulkBindRequest {
    private List<Long> markupPlanIds;
    private List<Long> businessIds;
}

