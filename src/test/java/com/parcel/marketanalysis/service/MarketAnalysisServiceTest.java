package com.parcel.marketanalysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.parcel.marketanalysis.client.DatasetClient;
import com.parcel.marketanalysis.client.EstimatorClient;
import com.parcel.marketanalysis.model.HousingRecord;
import com.parcel.marketanalysis.web.MarketFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MarketAnalysisServiceTest {
    @Mock
    DatasetClient datasetClient;

    @Mock
    EstimatorClient estimatorClient;

    @InjectMocks
    MarketAnalysisService service;

    @Test
    void summarizesAndFiltersSampleRows() {
        when(datasetClient.isModelLoaded()).thenReturn(true);
        when(datasetClient.getHouses()).thenReturn(List.of(
                house(1, 2, 185_000), house(2, 3, 265_000), house(3, 4, 345_000)));

        var summary = service.getSummary(new MarketFilter(null, null, 3, null, null, null));

        assertThat(summary.sampleSize()).isEqualTo(2);
        assertThat(summary.meanPrice()).isEqualTo(305_000);
        assertThat(summary.medianPrice()).isEqualTo(305_000);
    }

    @Test
    void rejectsInvertedFilterRanges() {
        when(datasetClient.isModelLoaded()).thenReturn(true);
        when(datasetClient.getHouses()).thenReturn(List.of(house(1, 2, 185_000)));

        assertThatThrownBy(() -> service.getSummary(new MarketFilter(2_000d, 1_000d, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Minimum square footage");
    }

    @Test
    void sortsAndPaginatesFilteredProperties() {
        when(datasetClient.isModelLoaded()).thenReturn(true);
        when(datasetClient.getHouses()).thenReturn(List.of(
                house(1, 2, 185_000), house(2, 3, 265_000), house(3, 4, 345_000)));

        var page = service.getProperties(new MarketFilter(null, null, null, null, null, null), 1, 1, "price", "desc");

        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.properties()).extracting(HousingRecord::id).containsExactly(2L);
    }

    @Test
    void returnsNoRowsAndEvictsCacheWhenModelIsUnavailable() {
        when(datasetClient.isModelLoaded()).thenReturn(false);

        var summary = service.getSummary(new MarketFilter(null, null, null, null, null, null));

        assertThat(summary.sampleSize()).isZero();
        verify(datasetClient).evictHousesCache();
        verify(datasetClient, never()).getHouses();
    }

    private HousingRecord house(long id, int bedrooms, double price) {
        return new HousingRecord(id, 1_500, bedrooms, 2, 2000, 6_000, 4, 7, price);
    }
}