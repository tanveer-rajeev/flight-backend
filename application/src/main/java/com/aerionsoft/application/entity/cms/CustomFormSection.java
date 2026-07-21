package com.aerionsoft.application.entity.cms;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "custom_form_section")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "form_id", nullable = false)
    @JsonBackReference
    private CustomForm form;

    @Column(name = "section_title", nullable = false)
    private String sectionTitle;

    @Column(name = "section_description", columnDefinition = "TEXT")
    private String sectionDescription;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CustomFormField> fields = new ArrayList<>();
}
