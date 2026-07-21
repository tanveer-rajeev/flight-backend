package com.aerionsoft.application.entity.cms;

import com.aerionsoft.application.util.UserDateTimeUtil;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_form")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(name = "banner_image", length = 500)
    private String bannerImage;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "form_status")
    private Integer formStatus = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt = UserDateTimeUtil.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = UserDateTimeUtil.now();

    @OneToMany(mappedBy = "form", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CustomFormSection> sections = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = UserDateTimeUtil.now();
    }
}
