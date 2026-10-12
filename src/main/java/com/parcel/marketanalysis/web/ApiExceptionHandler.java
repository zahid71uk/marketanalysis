package com.parcel.marketanalysis.web;

import com.parcel.marketanalysis.exception.UpstreamUnavailableException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(exception.getMessage(), Instant.now().toString()));
    }

    @ExceptionHandler(UpstreamUnavailableException.class)
    ResponseEntity<ApiError> upstreamUnavailable(UpstreamUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError(exception.getMessage(), Instant.now().toString()));
    }

    record ApiError(String message, String timestamp) {
    }
}