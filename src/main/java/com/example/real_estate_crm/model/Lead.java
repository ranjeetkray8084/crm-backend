package com.example.real_estate_crm.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "leads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leadId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({ "email", "password", "role", "phone", "createdAt", "updatedAt", "company", "notes",
            "leads" })
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @Column(nullable = false)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "reference_name")
    private String referenceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action = Action.UNASSIGNED;

    @ManyToOne
    @JoinColumn(name = "assigned_to") // ya "assigned_user_id" jo tumhara DB column hai
    @JsonBackReference("user-leads") // ✅ match with User.java
    private User assignedTo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private String location;

    @Version
    private Long version;

    @Column(precision = 15, scale = 2, nullable = true)
    private BigDecimal budget;

    @Column(columnDefinition = "TEXT")
    private String requirement;

    @OneToMany(mappedBy = "lead", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<LeadRemark> remarks;

    // Only return minimal createdBy info
    @JsonProperty("createdBy")
    public UserSummary getCreatedBySummary() {
        if (createdBy != null) {
            return new UserSummary(createdBy.getUserId(), createdBy.getName());
        }
        return null;
    }

    @JsonProperty("assignedToSummary")
    public UserSummary getAssignedToSummary() {
        if (assignedTo != null) {
            return new UserSummary(assignedTo.getUserId(), assignedTo.getName());
        }
        return null;
    }

    public Long getAssignedUserId() {
        return (assignedTo != null) ? assignedTo.getUserId() : null;
    }

    @JsonProperty("createdByName")
    public String getCreatedByName() {
        return createdBy != null ? createdBy.getName() : "Unknown";
    }

    public boolean isAssignable() {
        return this.action == Action.NEW || this.action == Action.UNASSIGNED;
    }

    public void assignTo(User user) {
        if (user == null)
            throw new IllegalArgumentException("User cannot be null");
        if (!isAssignable())
            throw new IllegalStateException("Lead must be NEW or UNASSIGNED to be assigned");
        this.assignedTo = user;
        this.action = Action.ASSIGNED;
    }

    public void unassign() {
        this.assignedTo = null;
        this.action = Action.UNASSIGNED;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime nowKolkata = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        this.createdAt = nowKolkata;
        this.updatedAt = nowKolkata;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    public enum Source {
        INSTAGRAM, FACEBOOK, YOUTUBE, REFERENCE, NINETY_NINE_ACRES, MAGIC_BRICKS
    }

    public enum Action {
        ASSIGNED, UNASSIGNED, NEW
    }

    public enum LeadStatus {
        NEW, CONTACTED, CLOSED, DROPED
    }
}
