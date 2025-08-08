package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "notes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // ID of the user who created the note

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Company company;

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime dateTime; // Scheduled date and time

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @ElementCollection
    @CollectionTable(name = "note_visible_users", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "user_id")
    private List<Long> visibleUserIds;

    @Enumerated(EnumType.STRING)
    private Status status; // NEW status field

    @Enumerated(EnumType.STRING)
    private Priority priority; // ✅ New priority field

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        if (this.status == null) {
            this.status = Status.NEW;
        }
        if (this.priority == null) {
            this.priority = Priority.PRIORITY_B; // ✅ Default to PRIORITY_B
        }
    }

    public enum Visibility {
        ONLY_ME,
        ME_AND_ADMIN,
        ALL_USERS,
        SPECIFIC_USERS,
        ME_AND_DIRECTOR,   // ✅ New
        SPECIFIC_ADMIN,    // ✅ New
        ALL_ADMIN          // ✅ New
    }


    public enum Status {
        NEW,
        PROCESSING,
        COMPLETED
    }

    public enum Priority {
        PRIORITY_A, 
        PRIORITY_B, 
        PRIORITY_C
    }
}
