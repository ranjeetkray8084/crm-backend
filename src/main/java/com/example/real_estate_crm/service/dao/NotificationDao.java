package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Notification;
import java.util.List;

public interface NotificationDao {
    
    // Method to fetch notifications by userId
    List<Notification> findByUserId(Long userId);

    // Method to save/send notification
    Notification save(Notification notification);
}
