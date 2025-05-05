package com.example.real_estate_crm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.real_estate_crm.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findByUserIdAndIsReadFalse(Long userId);
	  List<Notification> findByUserId(Long userId);
}
