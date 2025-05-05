package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

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

    @Column(columnDefinition = "TEXT")
    private String content;

    private LocalDateTime dateTime; // Scheduled date and time

    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    @ElementCollection
    @CollectionTable(name = "note_visible_users", joinColumns = @JoinColumn(name = "note_id"))
    @Column(name = "user_id")
    private List<Long> visibleUserIds; // List of specific users if visibility is SPECIFIC_USERS

    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Visibility {
        ONLY_ME,
        ME_AND_ADMIN,
        ALL_USERS,
        SPECIFIC_USERS
    }
}
