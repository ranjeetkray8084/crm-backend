package com.example.real_estate_crm.service;


import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.service.dao.NotificationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationDao notificationDao;

    // Method to fetch notifications by userId
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationDao.findByUserId(userId);
    }

    // Method to save/send a notification
    public void sendNotification(Long userId, String message) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setIsRead(false);  // By default, mark notification as unread
        notificationDao.save(notification);
    }
}
