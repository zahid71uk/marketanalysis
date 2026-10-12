package com.parcel.marketanalysis.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.parcel.marketanalysis.model.EstimateRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record WhatIfRequest(
        @JsonProperty("square_footage") @NotNull @Positive Double squareFootage,
        @NotNull @Min(0) Integer bedrooms,
        @NotNull @Min(0) Double bathrooms,
        @JsonProperty("year_built") @NotNull @Min(1800) @Max(2100) Integer yearBuilt,
        @JsonProperty("lot_size") @NotNull @Positive Double lotSize,
        @JsonProperty("distance_to_city_center") @NotNull @Min(0) Double distanceToCityCenter,
        @JsonProperty("school_rating") @NotNull @Min(0) @Max(10) Double schoolRating) {
    public EstimateRequest toEstimateRequest() {
        return new EstimateRequest(squareFootage, bedrooms, bathrooms, yearBuilt, lotSize,
                distanceToCityCenter, schoolRating);
    }
}