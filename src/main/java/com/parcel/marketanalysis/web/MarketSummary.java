package com.parcel.marketanalysis.web;

public record MarketSummary(
        int sampleSize,
        double meanPrice,
        double medianPrice,
        double minPrice,
        double maxPrice,
        double averageSquareFootage,
        double averageBedrooms) {
}