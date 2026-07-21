package com.aerionsoft.application.enums.audit;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityEventType {
    USER_LOGIN(ActivityEventCategory.AUTH, "User logged in"),
    ADMIN_LOGIN(ActivityEventCategory.AUTH, "Admin logged in"),
    LOGIN_FAILED(ActivityEventCategory.AUTH, "Login attempt failed"),
    TOKEN_REFRESH(ActivityEventCategory.AUTH, "Access token refreshed"),
    ADMIN_IMPERSONATE(ActivityEventCategory.AUTH, "Admin impersonated a user"),
    PASSWORD_RESET(ActivityEventCategory.AUTH, "Password reset completed"),
    PASSWORD_CHANGE(ActivityEventCategory.AUTH, "Password changed"),
    USER_REGISTERED(ActivityEventCategory.AUTH, "User registered"),

    ROLE_ASSIGNED(ActivityEventCategory.ACCESS, "Role assigned"),
    ROLE_REVOKED(ActivityEventCategory.ACCESS, "Role revoked"),

    DEPOSIT_APPROVED(ActivityEventCategory.WALLET, "Deposit approved"),
    DEPOSIT_REJECTED(ActivityEventCategory.WALLET, "Deposit rejected"),
    BALANCE_CREDIT(ActivityEventCategory.WALLET, "Balance credited"),
    BALANCE_DEBIT(ActivityEventCategory.WALLET, "Balance debited"),
    CREDIT_LIMIT_CHANGED(ActivityEventCategory.WALLET, "Credit limit changed"),

    BOOKING_CREATED(ActivityEventCategory.BOOKING, "Booking created"),
    BOOKING_CANCELLED(ActivityEventCategory.BOOKING, "Booking cancelled"),
    BOOKING_STATUS_CHANGED(ActivityEventCategory.BOOKING, "Booking status changed"),
    BOOKING_REFUNDED(ActivityEventCategory.BOOKING, "Booking refunded"),
    BOOKING_DELETED(ActivityEventCategory.BOOKING, "Booking deleted"),
    TICKET_ISSUED(ActivityEventCategory.BOOKING, "Ticket issued"),
    BOOKING_UPDATED(ActivityEventCategory.BOOKING, "Booking updated"),

    MARKUP_RULE_CREATED(ActivityEventCategory.FLIGHT, "Markup rule created"),
    MARKUP_RULE_UPDATED(ActivityEventCategory.FLIGHT, "Markup rule updated"),
    MARKUP_RULE_DELETED(ActivityEventCategory.FLIGHT, "Markup rule deleted"),
    MARKUP_PLAN_CREATED(ActivityEventCategory.FLIGHT, "Markup plan created"),
    MARKUP_PLAN_UPDATED(ActivityEventCategory.FLIGHT, "Markup plan updated"),
    MARKUP_PLAN_DELETED(ActivityEventCategory.FLIGHT, "Markup plan deleted"),

    PERMISSION_ASSIGNED(ActivityEventCategory.ACCESS, "Permissions assigned to role"),

    BUSINESS_APPROVED(ActivityEventCategory.USER, "Business approved"),
    BUSINESS_REJECTED(ActivityEventCategory.USER, "Business rejected"),

    CREDIT_REQUEST_APPROVED(ActivityEventCategory.WALLET, "Credit request approved"),
    CREDIT_REQUEST_REJECTED(ActivityEventCategory.WALLET, "Credit request rejected"),

    ADMIN_ACTION(ActivityEventCategory.ADMIN, "Admin action performed");

    private final ActivityEventCategory category;
    private final String defaultDescription;
}
