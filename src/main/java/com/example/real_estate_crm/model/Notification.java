package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    private String message;

    private boolean isRead = false;

    @Enumerated(EnumType.STRING)
    private NotificationType type = NotificationType.INFO;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

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
