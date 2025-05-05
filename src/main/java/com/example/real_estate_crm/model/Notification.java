package com.example.real_estate_crm.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String message;

    private boolean isRead = false;

    @Enumerated(EnumType.STRING) // Ensures it's stored as a string in the database
    private NotificationType type = NotificationType.INFO; // Default type

    private LocalDateTime createdAt = LocalDateTime.now();
    
    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }
    
    
    public enum NotificationType {
        INFO,
        WARNING,
        ALERT,
        SUCCESS
    }
}
