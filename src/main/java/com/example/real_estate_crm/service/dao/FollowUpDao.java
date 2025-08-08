package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.FollowUp;

import java.time.LocalDateTime;
import java.util.List;

public interface FollowUpDao {

    // Fetch all follow-ups for a given company
    List<FollowUp> getAllFollowUps(Long companyId);

    // Fetch a specific follow-up by ID and company ID
    FollowUp findById(Long companyId, Long id);

    // Add a new follow-up for a company
    FollowUp addFollowUp(Long companyId, FollowUp followUp);

    // Update a follow-up for a company
    FollowUp updateFollowUp(Long companyId, FollowUp followUp);

    // Delete a follow-up by ID and company ID
    void deleteById(Long companyId, Long id);

    // Fetch all follow-ups for today for a given company
    List<FollowUp> getTodayFollowUps(Long companyId, LocalDateTime start, LocalDateTime end);
}
