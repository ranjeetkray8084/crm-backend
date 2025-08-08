package com.example.real_estate_crm.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeadRemark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String remark;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    @JsonBackReference
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = true)
    @JsonIgnore // ❌ Don't expose full user
    private User createdBy;

    @PrePersist
    public void onCreate() {
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
