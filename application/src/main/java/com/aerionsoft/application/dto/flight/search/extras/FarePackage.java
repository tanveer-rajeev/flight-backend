package com.aerionsoft.application.dto.flight.search.extras;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class FarePackage {

    private String id;
    private String title;
    private List<String> included;
    private List<String> chargeableAddons;
    private FarePackagePolicies policies;
    private String price;
    private String type;
    private Double offerFare;

    private PackageFare packageFare;

    private List<PackageBaggage> packageBaggage;

    private List<FareBreakDown> packageFareBreakDowns;

}
