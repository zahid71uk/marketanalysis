package com.parcel.marketanalysis.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WhatIfResponse(@JsonProperty("predicted_price") double predictedPrice) {
}