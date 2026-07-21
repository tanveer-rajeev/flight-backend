package com.aerionsoft.application.dto.tour;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackageSearchResponse {
    private Page<TourPackageResponse> results;
    private List<TourPackageResponse> topSearched;
}
