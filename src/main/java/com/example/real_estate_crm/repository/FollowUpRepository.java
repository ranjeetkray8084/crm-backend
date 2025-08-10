package com.example.real_estate_crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.real_estate_crm.model.FollowUp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    // Fetch all follow-ups for a specific lead
    List<FollowUp> findByLeadLeadId(Long leadId);

    // Fetch all follow-ups for a specific company
    List<FollowUp> findByCompany_Id(Long companyId);

    // Fetch a follow-up by its ID and company ID
    Optional<FollowUp> findByFollowupIdAndCompany_Id(Long followupId, Long companyId);

    // ✅ Fixed today's follow-ups query
    @Query("SELECT f FROM FollowUp f " +
           "WHERE f.followupDate BETWEEN :startOfDay AND :endOfDay " +
           "AND f.company.Id = :companyId")
    List<FollowUp> findTodayFollowUps(
        @Param("companyId") Long companyId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    // ✅ Get all follow-ups for today across all companies (for daily reminders)
    @Query("SELECT f FROM FollowUp f " +
           "WHERE f.followupDate BETWEEN :startOfDay AND :endOfDay")
    List<FollowUp> findAllTodayFollowUps(
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    // ✅ Fetch follow-ups between two dates
    List<FollowUp> findByFollowupDateBetween(LocalDateTime start, LocalDateTime end);
}
