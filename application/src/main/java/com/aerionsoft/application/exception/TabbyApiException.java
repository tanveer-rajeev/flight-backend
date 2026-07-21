package com.aerionsoft.application.exception;

public class TabbyApiException extends RuntimeException {

    private final int statusCode;
    private final String rawBody;

    public TabbyApiException(int statusCode, String rawBody) {
        super("Tabby API error [%d]: %s".formatted(statusCode, rawBody));
        this.statusCode = statusCode;
        this.rawBody = rawBody;
    }

    public int statusCode() { return statusCode; }
    public String rawBody() { return rawBody; }
}
