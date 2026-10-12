package com.parcel.marketanalysis.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EstimateRequest(
        @JsonProperty("square_footage") double squareFootage,
        int bedrooms,
        double bathrooms,
        @JsonProperty("year_built") int yearBuilt,
        @JsonProperty("lot_size") double lotSize,
        @JsonProperty("distance_to_city_center") double distanceToCityCenter,
        @JsonProperty("school_rating") double schoolRating) {
}