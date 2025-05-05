package com.example.real_estate_crm.Controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.repository.NotificationRepository;
import com.example.real_estate_crm.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository repo;
    
    @Autowired
    private NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public List<Notification> getUnread(@PathVariable Long userId) {
        return repo.findByUserIdAndIsReadFalse(userId);
    }

    @PostMapping("/mark-as-read/{id}")
    public void markAsRead(@PathVariable Long id) {
        Notification notif = repo.findById(id).orElse(null);
        if (notif != null) {
            notif.setIsRead(true);
            repo.save(notif);
        }
    }
    
   

    @GetMapping("/{userId}")
    public List<Notification> getNotifications(@PathVariable Long userId) {
        return notificationService.getNotificationsByUserId(userId);
    }

    // Endpoint to send a notification to a user
    @PostMapping("/send")
    public void sendNotification(@RequestParam Long userId, @RequestParam String message) {
        notificationService.sendNotification(userId, message);
    }
}
