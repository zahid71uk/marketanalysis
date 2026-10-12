package com.parcel.marketanalysis.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EstimateResponse(@JsonProperty("predicted_price") double predictedPrice) {
}