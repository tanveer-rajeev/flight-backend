package com.aerionsoft.application.exception;

public class TabbyCheckoutRejectedException extends RuntimeException {

    private final String referenceId;
    private final String rejectionReason;

    public TabbyCheckoutRejectedException(String referenceId, String rejectionReason) {
        super("Tabby rejected checkout for reference %s: %s"
                .formatted(referenceId, rejectionReason != null ? rejectionReason : "no reason provided"));
        this.referenceId = referenceId;
        this.rejectionReason = rejectionReason;
    }

    public String referenceId() { return referenceId; }
    public String rejectionReason() { return rejectionReason; }
}
