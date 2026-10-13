package com.parcel.marketanalysis.web;

import com.parcel.marketanalysis.config.MarketAnalysisProperties;
import com.parcel.marketanalysis.service.MarketAnalysisService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/market-analysis")
public class MarketAnalysisController {
    private final MarketAnalysisService service;
    private final MarketAnalysisProperties properties;

    public MarketAnalysisController(MarketAnalysisService service, MarketAnalysisProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    @GetMapping("/summary")
    public MarketSummary summary(@Valid @ModelAttribute MarketFilter filter) {
        return service.getSummary(filter);
    }

    @GetMapping("/properties")
    public PagedProperties properties(
            @Valid @ModelAttribute MarketFilter filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "price") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return service.getProperties(filter, page, size, sortBy, direction);
    }

    @PostMapping("/what-if")
    public WhatIfResponse whatIf(@Valid @RequestBody WhatIfRequest request) {
        return service.estimate(request);
    }

    @PostMapping("/internal/dataset-cache")
    public ResponseEntity<Void> invalidateDatasetCache(
            @RequestHeader(name = "X-Dataset-Cache-Token", required = false) String suppliedToken) {
        String configuredToken = properties.cacheInvalidationToken();
        if (configuredToken == null || configuredToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (suppliedToken == null || !MessageDigest.isEqual(
                configuredToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        service.invalidateDatasetCache();
        return ResponseEntity.noContent().build();
    }
}