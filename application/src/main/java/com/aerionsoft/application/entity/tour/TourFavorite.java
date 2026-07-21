package com.aerionsoft.application.entity.tour;

import com.aerionsoft.application.util.UserDateTimeUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tour_favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tour_favorites", columnNames = {"user_id", "tour_package_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "tour_package_id", nullable = false)
    private Long tourPackageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_package_id", insertable = false, updatable = false)
    private TourPackage tourPackage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = UserDateTimeUtil.now();
    }
}
