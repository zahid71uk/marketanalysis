package com.parcel.marketanalysis.service;

import com.parcel.marketanalysis.client.DatasetClient;
import com.parcel.marketanalysis.client.EstimatorClient;
import com.parcel.marketanalysis.model.EstimateResponse;
import com.parcel.marketanalysis.model.HousingRecord;
import com.parcel.marketanalysis.web.MarketFilter;
import com.parcel.marketanalysis.web.MarketSummary;
import com.parcel.marketanalysis.web.PagedProperties;
import com.parcel.marketanalysis.web.WhatIfRequest;
import com.parcel.marketanalysis.web.WhatIfResponse;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Service;

@Service
public class MarketAnalysisService {
    private final DatasetClient datasetClient;
    private final EstimatorClient estimatorClient;

    public MarketAnalysisService(DatasetClient datasetClient, EstimatorClient estimatorClient) {
        this.datasetClient = datasetClient;
        this.estimatorClient = estimatorClient;
    }

    public MarketSummary getSummary(MarketFilter filter) {
        List<HousingRecord> houses = filter(datasetClient.getHouses(), filter);
        if (houses.isEmpty()) {
            return new MarketSummary(0, 0, 0, 0, 0, 0, 0);
        }
        List<Double> prices = houses.stream().map(HousingRecord::price).sorted().toList();
        double median = prices.size() % 2 == 0
                ? (prices.get(prices.size() / 2 - 1) + prices.get(prices.size() / 2)) / 2
                : prices.get(prices.size() / 2);
        return new MarketSummary(houses.size(), average(houses, HousingRecord::price), median,
                prices.getFirst(), prices.getLast(), average(houses, HousingRecord::squareFootage),
                average(houses, house -> house.bedrooms()));
    }

    public PagedProperties getProperties(MarketFilter filter, int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100.");
        }
        Comparator<HousingRecord> comparator = comparatorFor(sortBy);
        if ("desc".equalsIgnoreCase(direction)) {
            comparator = comparator.reversed();
        } else if (!"asc".equalsIgnoreCase(direction)) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'.");
        }
        List<HousingRecord> houses = filter(datasetClient.getHouses(), filter).stream()
                .sorted(comparator.thenComparingLong(HousingRecord::id))
                .toList();
        int start = (int) Math.min((long) page * size, houses.size());
        int end = Math.min(start + size, houses.size());
        int totalPages = (int) Math.ceil((double) houses.size() / size);
        return new PagedProperties(houses.subList(start, end), page, size, houses.size(), totalPages);
    }

    public WhatIfResponse estimate(WhatIfRequest request) {
        EstimateResponse response = estimatorClient.estimate(request.toEstimateRequest());
        return new WhatIfResponse(response.predictedPrice());
    }

    private List<HousingRecord> filter(List<HousingRecord> houses, MarketFilter filter) {
        validateRange(filter.minSquareFootage(), filter.maxSquareFootage(), "square footage");
        validateRange(filter.minBedrooms(), filter.maxBedrooms(), "bedrooms");
        validateRange(filter.minYearBuilt(), filter.maxYearBuilt(), "year built");
        return houses.stream()
                .filter(house -> filter.minSquareFootage() == null || house.squareFootage() >= filter.minSquareFootage())
                .filter(house -> filter.maxSquareFootage() == null || house.squareFootage() <= filter.maxSquareFootage())
                .filter(house -> filter.minBedrooms() == null || house.bedrooms() >= filter.minBedrooms())
                .filter(house -> filter.maxBedrooms() == null || house.bedrooms() <= filter.maxBedrooms())
                .filter(house -> filter.minYearBuilt() == null || house.yearBuilt() >= filter.minYearBuilt())
                .filter(house -> filter.maxYearBuilt() == null || house.yearBuilt() <= filter.maxYearBuilt())
                .toList();
    }

    private void validateRange(Number minimum, Number maximum, String label) {
        if (minimum != null && maximum != null && minimum.doubleValue() > maximum.doubleValue()) {
            throw new IllegalArgumentException("Minimum " + label + " cannot exceed maximum " + label + ".");
        }
    }

    private Comparator<HousingRecord> comparatorFor(String sortBy) {
        return switch (sortBy) {
            case "price" -> Comparator.comparingDouble(HousingRecord::price);
            case "squareFootage" -> Comparator.comparingDouble(HousingRecord::squareFootage);
            case "bedrooms" -> Comparator.comparingInt(HousingRecord::bedrooms);
            case "yearBuilt" -> Comparator.comparingInt(HousingRecord::yearBuilt);
            default -> throw new IllegalArgumentException("Unsupported sort field.");
        };
    }

    private double average(List<HousingRecord> houses, Function<HousingRecord, Number> value) {
        return houses.stream().mapToDouble(house -> value.apply(house).doubleValue()).average().orElse(0);
    }
}