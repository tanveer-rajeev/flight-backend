package com.aerionsoft.application.entity.client;

import com.aerionsoft.application.entity.BusinessEntity;
import com.aerionsoft.application.entity.HasCreatedUserTimestamp;
import com.aerionsoft.application.entity.listener.UserTimestampListener;
import com.aerionsoft.application.enums.user.UserType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Where;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "users")
@Where(clause = "is_deleted = false")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
@EntityListeners(UserTimestampListener.class)
public class User implements HasCreatedUserTimestamp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    @Email
    @NotBlank
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean isVerified = false;

    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = true)
    private String phoneNumber;

    @Column(nullable = true)
    private Double balance = 0.0;
    @Column(nullable = true)

    private String passportNumber;
    @Column(nullable = true)

    private String passportExpiryDate;
    @Column(nullable = true)

    private String image;

    @Column(nullable = true)
    private String currency = "USD";

    @Column(nullable = true)
    private LocalDate dob;

    @Column(nullable = true)
    private String address;

    @Column(nullable = true)
    private String Nationality;

    @Column(nullable = false)
    private Boolean isActive = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;

    @Column(nullable = true)
    private boolean isAgency = false;


    @Column(nullable = true)
    private String code;

    @Column(nullable = true)
    private boolean isDeleted = false;

    @Column(nullable = true)
    private String ticketPreview;


    @Column(nullable = true)
    private String invoicePreview;

    @ManyToOne
    @JoinColumn(name = "business_id")
    private BusinessEntity business;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_user_id")
    private User parentUser;

    @OneToMany(mappedBy = "parentUser", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<User> childrenUsers;

    @Column(nullable = true)
    private String voucherPreview;

    @Column(nullable = true)
    private String moneyReceiptPreview;

    @Column(nullable = true)
    private UserType userType;
}