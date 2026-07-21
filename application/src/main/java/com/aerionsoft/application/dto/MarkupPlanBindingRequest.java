package com.aerionsoft.application.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarkupPlanBindingRequest {
    private List<Long> markupPlanIds;
}

