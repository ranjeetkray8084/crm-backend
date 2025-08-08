package com.example.real_estate_crm.dto;

import java.time.LocalDateTime;

import com.example.real_estate_crm.model.UserSummary;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
 @Builder
public class TaskFileDTO {
    private Long id;
    private String title;
    private LocalDateTime uploadDate;
    private UserSummary assignedTo;
    private String uploadedByName;
}
