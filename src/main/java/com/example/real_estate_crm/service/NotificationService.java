package com.example.real_estate_crm.service;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.FollowUp;
import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.FollowUpRepository;
import com.example.real_estate_crm.repository.NotificationRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.PushNotificationService;
import com.example.real_estate_crm.service.PushTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private FollowUpRepository followUpRepository;
    
    @Autowired
    private PushTokenService pushTokenService;
    
    @Autowired
    private PushNotificationService pushNotificationService;

    // Send notification directly to a user (required by controller)
    public void sendNotification(Long userId, Company company, String message) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            System.err.println("❌ User not found with ID: " + userId);
            return;
        }

        // 🔒 SECURITY FIX: Company validation to prevent cross-company notifications
        if (!user.getCompany().getId().equals(company.getId())) {
            System.err.println("❌ SECURITY VIOLATION: User " + user.getEmail() + " (Company: " + user.getCompany().getId() + 
                             ") does not belong to company " + company.getId() + ". Notification blocked.");
            return;
        }

        try {
            // Create and save the in-app notification
            Notification notification = new Notification();
            notification.setUser(user);
            notification.setCompany(company);
            notification.setMessage(message);
            notification.setIsRead(false);
            notification.setCreatedAt(LocalDateTime.now());
            
            notificationRepository.save(notification);
            
            // 🔔 AUTOMATIC PUSH NOTIFICATION: Send push notification to ALL devices of this user
            try {
                var pushTokens = pushTokenService.getActivePushTokensByUser(user);
                if (!pushTokens.isEmpty()) {
                    System.out.println("🔔 Sending push notification to user: " + user.getEmail() + " (Company: " + company.getId() + 
                                     ") with " + pushTokens.size() + " devices");
                    
                    // Send to ALL devices of the user (not just first one)
                    for (var pushToken : pushTokens) {
                        try {
                            // Use the exact same message for both title and body to ensure consistency
                            String title = message; // Full message as title
                            String body = message;  // Full message as body
                            
                            pushNotificationService.sendPushNotification(
                                pushToken.getPushToken(),
                                title,
                                body,
                                Map.of(
                                    "type", "notification", 
                                    "notificationId", notification.getId(),
                                    "userId", userId,
                                    "companyId", company.getId(),
                                    "message", message // Include full message in data
                                )
                            );
                            
                            System.out.println("✅ Push notification sent to device: " + pushToken.getPushToken().substring(0, Math.min(20, pushToken.getPushToken().length())) + "...");
                        } catch (Exception deviceError) {
                            System.err.println("❌ Failed to send push notification to device: " + deviceError.getMessage());
                        }
                    }
                    
                    System.out.println("✅ Push notifications sent to " + pushTokens.size() + " devices for user: " + user.getEmail() + " (Company: " + company.getId() + ")");
                } else {
                    System.out.println("⚠️ No active push tokens found for user: " + user.getEmail() + " (Company: " + company.getId() + ")");
                }
            } catch (Exception pushError) {
                // Log push notification error but don't fail the main notification
                System.err.println("❌ Failed to send push notifications: " + pushError.getMessage());
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to create notification: " + e.getMessage());
        }
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

    // Get notifications by user & company
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


    // Delete old notifications (7 days)
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Kolkata")
    public void deleteOldNotifications() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);
        notificationRepository.deleteByCreatedAtBefore(oneWeekAgo);
    }

    // 🔹 Send notification for follow-up creation
    public void sendFollowUpCreatedNotification(FollowUp followUp, User creator) {
        String message = "📅 Follow-up created for lead \"" + followUp.getLead().getName() + 
                        "\" scheduled for " + followUp.getFollowupDate().toLocalDate() + 
                        " at " + followUp.getFollowupDate().toLocalTime().toString().substring(0, 5);
        
        // 🔔 AUTOMATIC: Notify the user who created the follow-up (will also send push notification)
        sendNotification(creator.getUserId(), followUp.getCompany(), message);
        
        // 🔔 AUTOMATIC: If creator is USER, notify their admin (will also send push notification)
        if (creator.getRole() == User.Role.USER && creator.getAdmin() != null) {
            sendNotification(creator.getAdmin().getUserId(), followUp.getCompany(), 
                           "📅 " + creator.getName() + " created a follow-up for lead \"" + 
                           followUp.getLead().getName() + "\"");
        }
        
        // 🔔 AUTOMATIC: Notify director (will also send push notification)
        User director = userRepository.findByRole(User.Role.DIRECTOR).stream()
                                    .filter(u -> u.getCompany().getId().equals(followUp.getCompany().getId()))
                                    .findFirst().orElse(null);
        if (director != null) {
            sendNotification(director.getUserId(), followUp.getCompany(), 
                           "📅 New follow-up created for lead \"" + followUp.getLead().getName() + "\"");
        }
    }

    // Follow-up reminders - Send only on the scheduled day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?", zone = "Asia/Kolkata")
    public void sendScheduledFollowUpReminders() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfDay = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
            
            // Get only today's follow-ups
            List<FollowUp> todayFollowUps = followUpRepository.findByFollowupDateBetween(
                startOfDay, endOfDay
            );
            
            if (todayFollowUps.isEmpty()) {
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
                        
                        // 🔔 AUTOMATIC: Send reminder to the user who created the follow-up (will also send push notification)
                        sendNotification(followUp.getUser().getUserId(), followUp.getCompany(), message);
                    }
                    
                } catch (Exception e) {
                    // Continue processing other follow-ups
                }
            }
            
        } catch (Exception e) {
            // Log error but don't fail the operation
        }
    }
}