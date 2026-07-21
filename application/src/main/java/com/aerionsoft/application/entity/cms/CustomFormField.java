package com.aerionsoft.application.entity.cms;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "custom_form_field")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    @JsonBackReference
    private CustomFormSection section;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @Column(name = "field_type", nullable = false, length = 50)
    private String fieldType; // text, number, date, file, select

    @Column(name = "is_required")
    private Boolean isRequired = false;

    @Column(name = "options", columnDefinition = "TEXT[]")
    private String[] options; // for dropdown/radio/checkbox

    @Column(name = "placeholder")
    private String placeholder;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;
}
