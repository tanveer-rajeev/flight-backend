package com.aerionsoft.application.entity.Booking;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.client.User;
import com.aerionsoft.application.enums.booking.TicketActionStatus;
import com.aerionsoft.application.enums.booking.TicketActionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_action_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketActionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketActionStatus status;

    @Column(length = 2000)
    private String reason;

    @Column(name = "admin_note", length = 2000)
    private String adminNote;

    // Quote details (admin provides)
    @Column(name = "quote_airline_cost", precision = 19, scale = 2)
    private BigDecimal quoteAirlineCost;

    @Column(name = "quote_service_charge", precision = 19, scale = 2)
    private BigDecimal quoteServiceCharge;

    @Column(name = "quote_total_amount", precision = 19, scale = 2)
    private BigDecimal quoteTotalAmount;

    @Column(name = "quote_currency", length = 10)
    private String quoteCurrency;

    @Column(name = "quote_exchange_rate", length = 10)
    private BigDecimal quoteExchangeRate;

    @Column(name = "quote_user_currency", length = 10)
    private String quoteUserCurrency;

    @Column(name = "quote_details", length = 2000)
    private String quoteDetails;

    @Column(name = "quoted_at")
    private LocalDateTime quotedAt;

    @Column(name = "quoted_by_admin_id")
    private Long quotedByAdminId;

    @Column(name = "user_confirmed_at")
    private LocalDateTime userConfirmedAt;

    @Column(name = "finalized_at")
    private LocalDateTime finalizedAt;

    @Column(name = "finalized_by_admin_id")
    private Long finalizedByAdminId;

    @Column(name = "final_result", length = 2000)
    private String finalResult;

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    // User must confirm before this deadline, otherwise request will be auto-rejected
    @Column(name = "accept_deadline")
    private LocalDateTime acceptDeadline;

    // Human-readable refund/void processing timeline (e.g., "3 days", "3 weeks")
    @Column(name = "refund_timeline", length = 50)
    private String refundTimeline;


    @Column(name = "is_refunded")
    private boolean refunded;

    /** Supplier cost kept on refund/void/cancel (USD), set when admin finalizes as COMPLETED */
    @Column(name = "supplier_refund_cost", precision = 19, scale = 2)
    private BigDecimal supplierRefundCost;

    /** buyPrice - supplierRefundCost, recorded at finalize */
    @Column(name = "supplier_payable_reversed", precision = 19, scale = 2)
    private BigDecimal supplierPayableReversed;

    /** Remaining supplier payable for the PNR (= supplierRefundCost) */
    @Column(name = "remaining_supplier_payable", precision = 19, scale = 2)
    private BigDecimal remainingSupplierPayable;

    /** Date the ticket was reissued with the airline (REISSUE type; set on submit, may be updated on finalize) */
    @Column(name = "reissue_date")
    private LocalDate reissueDate;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = UserDateTimeUtil.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }
}
