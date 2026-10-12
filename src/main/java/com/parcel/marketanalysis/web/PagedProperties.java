package com.parcel.marketanalysis.web;

import com.parcel.marketanalysis.model.HousingRecord;
import java.util.List;

public record PagedProperties(List<HousingRecord> properties, int page, int size, long totalElements, int totalPages) {
}