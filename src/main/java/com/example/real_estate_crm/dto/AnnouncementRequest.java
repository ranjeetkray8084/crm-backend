package com.example.real_estate_crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRequest {
    private String title;
    private String message;
    private LocalDateTime expiresAt;
    private Boolean sendPushNotification = true;
}
