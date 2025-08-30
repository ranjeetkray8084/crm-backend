package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(Long companyId);
    
    List<Announcement> findByCompanyIdAndPriorityAndIsActiveTrueOrderByCreatedAtDesc(Long companyId, Announcement.Priority priority);
    
    @Query("SELECT a FROM Announcement a WHERE a.company.id = :companyId AND a.isActive = true AND (a.expiresAt IS NULL OR a.expiresAt > :now) ORDER BY a.createdAt DESC")
    List<Announcement> findActiveAnnouncementsByCompany(@Param("companyId") Long companyId, @Param("now") LocalDateTime now);
    
    List<Announcement> findByCreatedByUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long userId);
    
    List<Announcement> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);
}
