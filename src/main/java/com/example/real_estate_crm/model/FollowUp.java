package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "follow_ups")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long followupId;

    @ManyToOne
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore  // hide full user object in JSON
    private User user;

    // Add company relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime followupDate;

    private LocalDateTime nextFollowup;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    // Expose only userId in JSON, not full User object
    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }
}
