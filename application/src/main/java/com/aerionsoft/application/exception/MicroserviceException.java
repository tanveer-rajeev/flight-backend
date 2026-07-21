package com.aerionsoft.application.exception;

import com.aerionsoft.application.enums.common.MicroserviceType;
import lombok.Getter;

@Getter
public class MicroserviceException extends RuntimeException {
    private final MicroserviceType serviceType;
    private final String errorCode;
    private final String serviceMessage;
    private final Object responseData;

    public MicroserviceException(MicroserviceType serviceType, String errorCode, String message) {
        super(message);
        this.serviceType = serviceType;
        this.errorCode = errorCode;
        this.serviceMessage = message;
        this.responseData = null;
    }

    public MicroserviceException(MicroserviceType serviceType, String errorCode, String message, Object responseData) {
        super(message);
        this.serviceType = serviceType;
        this.errorCode = errorCode;
        this.serviceMessage = message;
        this.responseData = responseData;
    }

    public MicroserviceException(MicroserviceType serviceType, String message, Object responseData) {
        super(message);
        this.serviceType = serviceType;
        this.errorCode = "UNKNOWN";
        this.serviceMessage = message;
        this.responseData = responseData;
    }
}
