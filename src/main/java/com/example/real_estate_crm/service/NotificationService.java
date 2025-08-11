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
        if (user == null) {
            System.out.println("❌ User not found with ID: " + userId);
            return;
        }

        System.out.println("📧 Creating notification for user: " + user.getName() + " (ID: " + userId + ") in company: " + company.getName());
        System.out.println("📝 Message: " + message);

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setCompany(company);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        System.out.println("✅ Notification saved with ID: " + saved.getId());
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
        System.out.println("🔍 Searching notifications for user: " + user.getName() + " (ID: " + user.getUserId() + ") in company: " + company.getName() + " (ID: " + company.getId() + ")");
        
        List<Notification> notifications = notificationRepository.findByUserAndCompany(user, company)
                                     .stream()
                                     .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                                     .toList();
        
        System.out.println("📧 Retrieved " + notifications.size() + " notifications from database");
        return notifications;
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

    // 🔹 Follow-up reminders - Send only on the scheduled day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void sendScheduledFollowUpReminders() {
        System.out.println("🔔 Checking for today's scheduled follow-ups to send reminders...");
        
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
            
            // Get only today's follow-ups (जिस दिन follow-up scheduled है)
            List<FollowUp> todayFollowUps = followUpRepository.findByFollowupDateBetween(
                startOfDay, endOfDay
            );
            
            System.out.println("📅 Found " + todayFollowUps.size() + " follow-ups scheduled for today (" + 
                             now.toLocalDate() + ")");
            
            if (todayFollowUps.isEmpty()) {
                System.out.println("✅ No follow-ups scheduled for today, no reminders to send");
                return;
            }
            
            for (FollowUp followUp : todayFollowUps) {
                try {
                    // Only send reminder if follow-up is scheduled for today
                    LocalDateTime followUpDate = followUp.getFollowupDate();
                    if (followUpDate.toLocalDate().equals(now.toLocalDate())) {
                        
                        String timeStr = followUpDate.toLocalTime().toString();
                        if (timeStr.length() > 5) {
                            timeStr = timeStr.substring(0, 5);
                        }
                        
                        String message = "⏰ Reminder: You have a follow-up TODAY for lead \"" + 
                                       followUp.getLead().getName() + "\" at " + timeStr;
                        
                        // Send reminder to the user who created the follow-up
                        sendNotification(followUp.getUser().getUserId(), followUp.getCompany(), message);
                        
                        System.out.println("📧 Reminder sent to " + followUp.getUser().getName() + 
                                         " for lead: " + followUp.getLead().getName() + 
                                         " scheduled at: " + timeStr);
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Failed to send reminder for follow-up ID: " + followUp.getFollowupId() + 
                                     " - " + e.getMessage());
                }
            }
            
            System.out.println("✅ Today's follow-up reminders completed");
            
        } catch (Exception e) {
            System.err.println("❌ Error in sendScheduledFollowUpReminders: " + e.getMessage());
            e.printStackTrace();
        }
    }
}