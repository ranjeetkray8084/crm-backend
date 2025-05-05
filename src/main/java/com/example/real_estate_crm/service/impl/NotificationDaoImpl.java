package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.repository.NotificationRepository;
import com.example.real_estate_crm.service.dao.NotificationDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationDaoImpl implements NotificationDao {

    @Autowired
    private NotificationRepository notificationRepository;

    // Method to fetch notifications by userId
    @Override
    public List<Notification> findByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    // Method to save/send notification
    @Override
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }
}
