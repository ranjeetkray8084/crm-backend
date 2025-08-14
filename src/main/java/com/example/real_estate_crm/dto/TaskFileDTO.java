package com.example.real_estate_crm.dto;

import java.time.LocalDateTime;

import com.example.real_estate_crm.model.UserSummary;
import com.example.real_estate_crm.model.TaskFile.TaskStatus;

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
    private Long uploadedBy; // User ID who uploaded the file
    private String uploadedByName;
    private TaskStatus status;
    private LocalDateTime assignedToDate;
}
