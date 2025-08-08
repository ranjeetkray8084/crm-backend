package com.example.real_estate_crm.dto;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowUpRequest {
    private String note;
    private LocalDateTime followUpDate;
    private Long leadId;
    private Long userId;
}
