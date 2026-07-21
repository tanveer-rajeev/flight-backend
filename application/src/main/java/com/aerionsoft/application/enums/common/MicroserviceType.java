package com.aerionsoft.application.enums.common;

import lombok.Getter;

@Getter
public enum MicroserviceType {
    CORE_BOOKING("core-booking"),
    PAYMENT_SERVICE("payment"),
    NOTIFICATION_SERVICE("notification"),
    USER_SERVICE("user"),
    VISA_SERVICE("visa"),
    FLIGHT_SERVICE("flight"),
    WALLET_SERVICE("wallet");

    private final String serviceName;

    MicroserviceType(String serviceName) {
        this.serviceName = serviceName;
    }

}
