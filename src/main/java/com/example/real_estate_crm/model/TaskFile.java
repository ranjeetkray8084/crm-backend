package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "task_file")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title; // Excel file ka naam + logical title (same)

    @Column(name = "file_path")
    private String filePath; // Full file path saved on server

    @Column(name = "assigned_to", nullable = true)
    private Long assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploadedBy", nullable = false)
    @JsonIgnoreProperties({"email", "password", "role", "phone", "createdAt", "updatedAt", "company", "notes", "leads"})
    private User uploadedBy;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "upload_date")
    private LocalDateTime uploadDate = LocalDateTime.now();
    
    @JsonProperty("createdBy")
    public UserSummary getCreatedBySummary() {
        if (uploadedBy != null) {
            return new UserSummary(uploadedBy.getUserId(), uploadedBy.getName());
        }
        return null;
    }

    @JsonProperty("createdByName")
    public String getCreatedByName() {
        return uploadedBy != null ? uploadedBy.getName() : "Unknown";
    }

}
