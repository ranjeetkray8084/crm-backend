package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Entity
@Table(name = "note_remarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoteRemark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ID of the user who added the remark

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Note note;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String remark;

    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    @JsonIgnore // ❌ Don't expose full user
    private User createdBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }
    
    // ✅ Only send userId and name in response
    @JsonProperty("createdBy")
    public UserSummary getCreatedBySummary() {
        if (createdBy != null) {
            return new UserSummary(createdBy.getUserId(), createdBy.getName());
        }
        return null;
    }
}
