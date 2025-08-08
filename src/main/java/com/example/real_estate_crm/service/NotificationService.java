package com.example.real_estate_crm.service;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.NotificationRepository;
import com.example.real_estate_crm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

 // 🔹 Send notification directly to a user (required by controller)
    public void sendNotification(Long userId, Company company, String message) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setCompany(company);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }



    // 🔹 Create & Save Notification
    public void createAndSaveNotification(User user, Company company, String message) {
        Notification notification = new Notification();
        notification.setUser(user); // ✅ FIXED
        notification.setCompany(company);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

 // 🔹 Get notifications by user & company
    public List<Notification> getNotificationsByUserAndCompany(User user, Company company) {
        return notificationRepository.findByUserAndCompany(user, company)
                                     .stream()
                                     .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                     .toList();
    }

    // 🔹 Mark all notifications as read
    @Transactional
    public void markAllAsRead(User user, Company company) {
        List<Notification> notifications = notificationRepository.findByUserAndCompany(user, company)
                                                                 .stream()
                                                                 .filter(n -> !n.isRead())
                                                                 .toList();
        for (Notification notif : notifications) {
            notif.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
    }


    // 🔹 Delete old notifications (7 days)
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Kolkata")
    public void deleteOldNotifications() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        notificationRepository.deleteByCreatedAtBefore(oneWeekAgo);
        System.out.println("🔔 Deleted notifications created before: " + oneWeekAgo);
    }
}