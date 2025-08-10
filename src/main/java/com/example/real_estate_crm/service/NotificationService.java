package com.example.real_estate_crm.service;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.FollowUp;
import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.FollowUpRepository;
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
    
    @Autowired
    private FollowUpRepository followUpRepository;

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

    // 🔹 Send notification for follow-up creation
    public void sendFollowUpCreatedNotification(FollowUp followUp, User creator) {
        String message = "📅 Follow-up created for lead \"" + followUp.getLead().getName() + 
                        "\" scheduled for " + followUp.getFollowupDate().toLocalDate() + 
                        " at " + followUp.getFollowupDate().toLocalTime().toString().substring(0, 5);
        
        // Notify the user who created the follow-up
        sendNotification(creator.getUserId(), followUp.getCompany(), message);
        
        // If creator is USER, notify their admin
        if (creator.getRole() == User.Role.USER && creator.getAdmin() != null) {
            sendNotification(creator.getAdmin().getUserId(), followUp.getCompany(), 
                           "📅 " + creator.getName() + " created a follow-up for lead \"" + 
                           followUp.getLead().getName() + "\"");
        }
        
        // Notify director
        User director = userRepository.findByRole(User.Role.DIRECTOR).stream()
                                    .filter(u -> u.getCompany().getId().equals(followUp.getCompany().getId()))
                                    .findFirst().orElse(null);
        if (director != null) {
            sendNotification(director.getUserId(), followUp.getCompany(), 
                           "📅 New follow-up created for lead \"" + followUp.getLead().getName() + "\"");
        }
    }

    // 🔹 Daily follow-up reminders DISABLED per request
    // @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void sendDailyFollowUpReminders() {
        // Intentionally left blank
        // Daily follow-up reminder notifications are disabled.
    }
}