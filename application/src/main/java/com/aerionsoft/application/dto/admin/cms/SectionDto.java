package com.aerionsoft.application.dto.admin.cms;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Section DTO for JSON serialization within Content
 * This represents a section embedded in content as JSON
 */
public class SectionDto {
    
    @NotBlank(message = "Section title is required")
    @JsonProperty("title")
    private String title;
    
    @NotBlank(message = "Section description is required")
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("imageUrl")
    private String imageUrl;
    
    // Default constructor
    public SectionDto() {}
    
    // Constructor with all fields
    public SectionDto(String title, String description, String imageUrl) {
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    @Override
    public String toString() {
        return "SectionDto{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
