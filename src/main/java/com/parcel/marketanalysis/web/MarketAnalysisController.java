package com.parcel.marketanalysis.web;

import com.parcel.marketanalysis.service.MarketAnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market-analysis")
public class MarketAnalysisController {
    private final MarketAnalysisService service;

    public MarketAnalysisController(MarketAnalysisService service) {
        this.service = service;
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
}