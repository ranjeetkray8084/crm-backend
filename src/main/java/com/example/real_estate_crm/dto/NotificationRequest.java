package com.example.real_estate_crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private List<Long> userIds; // Send to specific users
    private Long companyId; // Send to all users in company
    private String title;
    private String body;
    private Map<String, Object> data; // Additional data for deep linking
    private String sound = "default";
    private Integer badge;
    private String channelId = "default";
}
