package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.PushNotificationService;
import com.example.real_estate_crm.service.PushTokenService;
import com.example.real_estate_crm.service.dao.UserDao;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository repo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private PushNotificationService pushNotificationService;
    
    @Autowired
    private PushTokenService pushTokenService;

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId).orElse(null);
    }

    private User getUser(Long userId) {
        return userDao.findById(userId).orElse(null);
    }

    @GetMapping("/user/{userId}/company/{companyId}/unread")
    public ResponseEntity<List<Notification>> getUnreadByUserAndCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Notification> notifications = repo.findByUserAndCompany(user, company)
                                               .stream()
                                               .filter(n -> !n.isRead())
                                               .toList();

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/company/{companyId}/unread-count")
    public ResponseEntity<Long> getUnreadCountByUserAndCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0L);
        }

        long count = repo.findByUserAndCompany(user, company)
                         .stream()
                         .filter(n -> !n.isRead())
                         .count();

        return ResponseEntity.ok(count);
    }

    @GetMapping("/user/{userId}/company/{companyId}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAndCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Notification> notifications = notificationService.getNotificationsByUserAndCompany(user, company);
        
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/mark-all-as-read/user/{userId}/company/{companyId}")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company or User not found");
        }

        notificationService.markAllAsRead(user, company);
        return ResponseEntity.ok("All notifications marked as read.");
    }

    @PostMapping("/mark-as-read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Notification notif = repo.findById(id).orElse(null);
        if (notif == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found");
        }
        notif.setIsRead(true);
        repo.save(notif);
        return ResponseEntity.ok(notif);
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(
            @RequestParam Long actorUserId,
            @RequestParam Long companyId,
            @RequestParam String message) {

        Company company = getCompany(companyId);
        User actor = getUser(actorUserId);

        if (company == null || actor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Actor user or Company not found.");
        }

        // 🔔 Notify Admin (if actor is USER)
        if (actor.getRole() == User.Role.USER && actor.getAdmin() != null) {
            notificationService.sendNotification(actor.getAdmin().getUserId(), company, message);
        }

        // 🔔 Notify Director
        User director = userDao.findDirectorByCompany(company);
        if (director != null) {
            notificationService.sendNotification(director.getUserId(), company, message);
        }

        return ResponseEntity.ok("Notification sent to relevant users.");
    }

    /**
     * 🔔 TEST ENDPOINT: Test notification with automatic push notification
     * This endpoint demonstrates the automatic push notification feature
     */
    @PostMapping("/test-with-push")
    public ResponseEntity<String> testNotificationWithPush(
            @RequestParam Long targetUserId,
            @RequestParam Long companyId,
            @RequestParam String message) {

        Company company = getCompany(companyId);
        User targetUser = getUser(targetUserId);

        if (company == null || targetUser == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Target user or Company not found.");
        }

        // This will automatically send both in-app notification AND push notification
        notificationService.sendNotification(targetUserId, company, message);
        
        return ResponseEntity.ok("✅ Test notification sent! Check both in-app notifications and push notifications.");
    }

    /**
     * 🚀 NEW: Send bulk notifications to multiple users with push notifications
     */
    @PostMapping("/bulk-send")
    public ResponseEntity<Map<String, Object>> sendBulkNotifications(
            @RequestParam Long companyId,
            @RequestParam List<Long> userIds,
            @RequestParam String message,
            @RequestParam(defaultValue = "false") boolean sendPushNotification) {

        Company company = getCompany(companyId);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Company not found"));
        }

        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int pushSuccessCount = 0;
        List<String> failedUsers = new ArrayList<>();
        List<String> securityViolations = new ArrayList<>();

        for (Long userId : userIds) {
            User user = getUser(userId);
            if (user != null) {
                // 🔒 SECURITY FIX: Verify user belongs to the specified company
                if (!user.getCompany().getId().equals(companyId)) {
                    String violation = "User " + user.getEmail() + " (Company: " + user.getCompany().getId() + 
                                     ") does not belong to company " + companyId;
                    securityViolations.add(violation);
                    System.err.println("❌ SECURITY VIOLATION: " + violation);
                    continue;
                }

                try {
                    // Send regular notification (this automatically sends push notification too)
                    notificationService.sendNotification(userId, company, message);
                    successCount++;
                    
                    // Track push notification success if requested
                    if (sendPushNotification) {
                        var pushTokens = pushTokenService.getActivePushTokensByUser(user);
                        if (!pushTokens.isEmpty()) {
                            pushSuccessCount++;
                        }
                    }
                } catch (Exception e) {
                    failedUsers.add(user.getEmail() + ": " + e.getMessage());
                }
            }
        }

        result.put("message", "Bulk notifications sent");
        result.put("totalUsers", userIds.size());
        result.put("successCount", successCount);
        result.put("pushSuccessCount", pushSuccessCount);
        result.put("failedUsers", failedUsers);
        result.put("securityViolations", securityViolations);
        result.put("companyId", companyId);

        return ResponseEntity.ok(result);
    }

    /**
     * 🎯 NEW: Send priority-based notification with enhanced push notification
     */
    @PostMapping("/send-priority")
    public ResponseEntity<Map<String, Object>> sendPriorityNotification(
            @RequestParam Long userId,
            @RequestParam Long companyId,
            @RequestParam String message,
            @RequestParam String priority) {

        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User or Company not found"));
        }

        try {
            // Send regular notification (automatically includes push notification)
            notificationService.sendNotification(userId, company, message);
            
            // Send additional high-priority push notification if needed
            if ("HIGH".equalsIgnoreCase(priority) || "URGENT".equalsIgnoreCase(priority)) {
                var pushTokens = pushTokenService.getActivePushTokensByUser(user);
                if (!pushTokens.isEmpty()) {
                    // Send high-priority push notification
                    for (var pushToken : pushTokens) {
                        pushNotificationService.sendPushNotification(
                            pushToken.getPushToken(),
                            "🚨 " + priority + " Priority: " + message,
                            message,
                            Map.of(
                                "type", "priority_notification",
                                "priority", priority,
                                "notificationId", "priority_" + System.currentTimeMillis(),
                                "userId", userId,
                                "companyId", companyId,
                                "message", message
                            )
                        );
                    }
                }
            }

            return ResponseEntity.ok(Map.of(
                "message", "Priority notification sent successfully",
                "priority", priority,
                "pushNotificationSent", true
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to send priority notification",
                "details", e.getMessage()
            ));
        }
    }

    /**
     * 📱 NEW: Get notification history with push notification status
     */
    @GetMapping("/history/{userId}/company/{companyId}")
    public ResponseEntity<Map<String, Object>> getNotificationHistory(
            @PathVariable Long userId,
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "10") int limit) {

        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User or Company not found"));
        }

        try {
            // Get notifications
            List<Notification> notifications = notificationService.getNotificationsByUserAndCompany(user, company);
            
            // Get user's push tokens to show push notification capability
            var pushTokens = pushTokenService.getActivePushTokensByUser(user);
            boolean hasPushTokens = !pushTokens.isEmpty();
            
            // Limit notifications
            List<Notification> limitedNotifications = notifications.stream()
                .limit(limit)
                .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("notifications", limitedNotifications);
            result.put("totalCount", notifications.size());
            result.put("hasPushTokens", hasPushTokens);
            result.put("pushTokenCount", pushTokens.size());
            result.put("pushNotificationEnabled", hasPushTokens);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to get notification history",
                "details", e.getMessage()
            ));
        }
    }

    /**
     * 🔔 NEW: Send notification template with push notification
     */
    @PostMapping("/send-template")
    public ResponseEntity<Map<String, Object>> sendTemplateNotification(
            @RequestParam Long userId,
            @RequestParam Long companyId,
            @RequestParam String templateType,
            @RequestParam Map<String, String> templateData) {

        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User or Company not found"));
        }

        try {
            String message = buildTemplateMessage(templateType, templateData);
            
            // Send notification (automatically includes push notification)
            notificationService.sendNotification(userId, company, message);
            
            return ResponseEntity.ok(Map.of(
                "message", "Template notification sent successfully",
                "templateType", templateType,
                "generatedMessage", message,
                "pushNotificationSent", true
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to send template notification",
                "details", e.getMessage()
            ));
        }
    }

    /**
     * Helper method to build template messages
     */
    private String buildTemplateMessage(String templateType, Map<String, String> data) {
        switch (templateType.toLowerCase()) {
            case "welcome":
                return "🎉 Welcome to " + data.getOrDefault("companyName", "our CRM") + "! We're excited to have you on board.";
            
            case "lead_assigned":
                return "🎯 New lead '" + data.getOrDefault("leadName", "Lead") + "' has been assigned to you.";
            
            case "task_due":
                return "⏰ Task '" + data.getOrDefault("taskName", "Task") + "' is due on " + data.getOrDefault("dueDate", "soon");
            
            case "follow_up":
                return "📅 Follow-up reminder for lead '" + data.getOrDefault("leadName", "Lead") + "' scheduled for " + data.getOrDefault("date", "today");
            
            case "property_update":
                return "🏠 Property '" + data.getOrDefault("propertyName", "Property") + "' has been " + data.getOrDefault("action", "updated");
            
            default:
                return data.getOrDefault("message", "Notification from CRM");
        }
    }

}
