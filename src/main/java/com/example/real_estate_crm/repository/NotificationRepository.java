package com.example.real_estate_crm.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.Company;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ✅ Find unread notifications for user in specific company
    List<Notification> findByUserAndCompanyAndIsReadFalse(User user, Company company);

    // ✅ Find all notifications for user in specific company
    List<Notification> findByUserAndCompany(User user, Company company);

    // ✅ Find all notifications for user in company ordered by creation date descending
    List<Notification> findByUserAndCompanyOrderByCreatedAtDesc(User user, Company company);

    // ✅ Count unread notifications
    long countByUserAndCompanyAndIsReadFalse(User user, Company company);

    // ✅ Delete old notifications
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
