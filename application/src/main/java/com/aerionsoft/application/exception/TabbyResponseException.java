package com.aerionsoft.application.exception;

public class TabbyResponseException extends RuntimeException {

    private final String orderId;
    private final String tabbyStatus;

    public TabbyResponseException(String orderId, String tabbyStatus, String detail) {
        super("Unexpected Tabby response for order %s (status=%s): %s".formatted(orderId, tabbyStatus, detail));
        this.orderId = orderId;
        this.tabbyStatus = tabbyStatus;
    }

    public String orderId() { return orderId; }
    public String tabbyStatus() { return tabbyStatus; }
}
