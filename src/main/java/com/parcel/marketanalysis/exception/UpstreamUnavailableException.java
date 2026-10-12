package com.parcel.marketanalysis.exception;

public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String service, Throwable cause) {
        super(service + " is unavailable.", cause);
    }
}