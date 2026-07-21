package com.aerionsoft.application.entity.tour;

import com.aerionsoft.application.enums.tour.TourFlag;
import com.aerionsoft.application.util.UserDateTimeUtil;

import com.aerionsoft.application.entity.Media;
import com.aerionsoft.application.entity.PackageItem;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "tour_packages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "full_description", columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "destination_city", nullable = false)
    private String destinationCity;

    @Column(name = "destination_country", nullable = false)
    private String destinationCountry;

    @Column(name = "map_location", columnDefinition = "TEXT")
    private String mapLocation;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageStatus status = PackageStatus.DRAFT;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "from_id")
    private Long fromId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id")
    private TourPackageType type;

    @Column(name = "search_count", nullable = false)
    private Long searchCount = 0L;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tour_package_flags", joinColumns = @JoinColumn(name = "tour_package_id"))
    @Column(name = "flag")
    @Enumerated(EnumType.STRING)
    private Set<TourFlag> flags = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tour_package_categories",
        joinColumns = @JoinColumn(name = "tour_package_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<TourCategory> categories = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_time_offset", length = 32)
    private String createdTimeOffset;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_time_offset", length = 32)
    private String updatedTimeOffset;

    @OneToMany(mappedBy = "tourPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Media> media = new ArrayList<>();

    @OneToMany(mappedBy = "tourPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Pricing> pricing = new ArrayList<>();

    @OneToMany(mappedBy = "tourPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<PackageItem> packageItems = new ArrayList<>();

    @OneToMany(mappedBy = "tourPackage", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<Itinerary> itineraries = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
        updatedAt = UserDateTimeUtil.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = UserDateTimeUtil.now();
    }

    public enum PackageStatus {
        DRAFT, PUBLISHED, INACTIVE
    }
}
