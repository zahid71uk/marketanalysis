package com.parcel.marketanalysis.model;

import java.util.List;

public record DatasetResponse(List<HousingRecord> houses, int count) {
}