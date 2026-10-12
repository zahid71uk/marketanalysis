package com.parcel.marketanalysis.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record MarketFilter(
        Double minSquareFootage,
        Double maxSquareFootage,
        @Min(0) Integer minBedrooms,
        @Min(0) Integer maxBedrooms,
        @Min(1800) @Max(2100) Integer minYearBuilt,
        @Min(1800) @Max(2100) Integer maxYearBuilt) {
}