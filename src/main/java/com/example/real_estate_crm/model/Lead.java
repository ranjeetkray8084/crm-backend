package com.example.real_estate_crm.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "`lead`")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long leadId;
    
    @Column(nullable = false)
    private String createdBy;

    @NotBlank(message = "Name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number format")
    @Column(nullable = false)
    private String phone;

    @NotBlank(message = "Source is required")
    @Column(nullable = false)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action = Action.UNASSIGNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to")
    @JsonBackReference // Prevent recursion on this side of the relationship
    private User assignedTo;
    
   

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Column(precision = 15, scale = 2)
    private String budget;

    @Column(columnDefinition = "TEXT")
    private String requirement;

    @Column(columnDefinition = "TEXT")
    private String remark;

    // Business Logic
    public boolean isAssignable() {
        return this.action == Action.NEW || this.action == Action.UNASSIGNED;
    }

    public void assignTo(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (!isAssignable()) {
            throw new IllegalStateException("Lead must be NEW or UNASSIGNED to be assigned");
        }
        this.assignedTo = user;
        this.action = Action.ASSIGNED;
    }

    public void unassign() {
        this.assignedTo = null;
        this.action = Action.UNASSIGNED;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ Assigned user summary for frontend display
    @JsonProperty("assignedToSummary")
    public Object getAssignedToSummary() {
        if (assignedTo != null) {
            return new Object() {
                public final Long userId = assignedTo.getUserId();
                public final String name = assignedTo.getName();
            };
        }
        return null;
    }

    // Getter for the assigned user ID (for the Lead class)
    public Long getAssignedUserId() {
        return (assignedTo != null) ? assignedTo.getUserId() : null;
    }

    public enum Source {
        INSTAGRAM, FACEBOOK, YOUTUBE, REFERENCE
    }

    public enum Action {
        ASSIGNED, UNASSIGNED, NEW
    }

    public enum LeadStatus {
        NEW, CONTACTED, CLOSED
    }
}
