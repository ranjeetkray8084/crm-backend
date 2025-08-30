package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.NotificationRequest;
import com.example.real_estate_crm.dto.PushTokenRequest;
import com.example.real_estate_crm.model.PushToken;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.service.PushNotificationService;
import com.example.real_estate_crm.service.PushTokenService;
import com.example.real_estate_crm.service.dao.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/push-notifications")
@RequiredArgsConstructor
@Slf4j
public class PushNotificationController {

    private final PushTokenService pushTokenService;
    private final PushNotificationService pushNotificationService;
    private final UserDao userDao;

    /**
     * Register push token for current user
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerPushToken(
            @RequestBody PushTokenRequest request,
            Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            pushTokenService.registerPushToken(user, request.getPushToken(), request.getDeviceType());
            
            log.info("✅ Push token registered for user: {}", user.getEmail());
            return ResponseEntity.ok(Map.of("message", "Push token registered successfully"));
        } catch (Exception e) {
            log.error("❌ Failed to register push token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to register push token"));
        }
    }

    /**
     * Send push notification to specific users
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendPushNotification(
            @RequestBody NotificationRequest request,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("🔔 Push notification requested by user: {} for company: {}", 
                currentUser.getEmail(), currentUser.getCompany().getId());
            
            // Validate request
            if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
            }
            
            if (request.getBody() == null || request.getBody().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Body is required"));
            }
            
            // Only allow sending to specific users, not to entire company
            if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
                // Send to specific users
                List<String> pushTokens = pushTokenService.getPushTokensByUserIds(request.getUserIds());
                if (!pushTokens.isEmpty()) {
                    pushNotificationService.sendPushNotificationToMultiple(
                        pushTokens, 
                        request.getTitle(), 
                        request.getBody(), 
                        request.getData()
                    );
                    log.info("🔔 Push notification sent to {} specific users", pushTokens.size());
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "Push notification sent successfully",
                        "recipientsCount", pushTokens.size(),
                        "targets", Map.of(
                            "users", request.getUserIds().size(),
                            "companyWide", false
                        )
                    ));
                } else {
                    log.warn("⚠️ No active push tokens found for the specified users");
                    return ResponseEntity.badRequest().body(Map.of("error", "No active push tokens found for the specified users"));
                }
            } else {
                // Company-wide notifications are not allowed for security reasons
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Company-wide notifications are not allowed. Please specify specific user IDs.",
                    "message", "For security reasons, you must specify which users should receive the notification"
                ));
            }
        } catch (Exception e) {
            log.error("❌ Failed to send push notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send push notification"));
        }
    }

    /**
     * Test push notification (for development)
     */
    @PostMapping("/test")
    public ResponseEntity<?> testPushNotification(
            @RequestBody PushTokenRequest request,
            Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("🧪 Test push notification requested by user: {} with token: {}...", 
                user.getEmail(), request.getPushToken().substring(0, Math.min(20, request.getPushToken().length())));
            
            // Check if service is available
            if (pushNotificationService == null) {
                log.error("❌ PushNotificationService is null!");
                return ResponseEntity.badRequest().body(Map.of("error", "PushNotificationService not available"));
            }
            
            log.info("🔔 Calling pushNotificationService.sendPushNotification...");
            
            // Send test notification
            boolean success = pushNotificationService.sendPushNotification(
                request.getPushToken(),
                "Test Notification",
                "This is a test push notification from your CRM app!",
                Map.of("type", "test", "timestamp", System.currentTimeMillis())
            );
            
            log.info("🧪 Test push notification result: {}", success ? "SUCCESS" : "FAILED");
            
            if (success) {
                log.info("✅ Test push notification sent successfully to user: {}", user.getEmail());
                return ResponseEntity.ok(Map.of("message", "Test notification sent successfully"));
            } else {
                log.error("❌ Test push notification failed for user: {}", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of("error", "Test notification failed"));
            }
        } catch (Exception e) {
            log.error("❌ Failed to send test notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send test notification: " + e.getMessage()));
        }
    }

    /**
     * Test FCM notification specifically (for debugging)
     */
    @PostMapping("/test-fcm")
    public ResponseEntity<?> testFCMNotification(
            @RequestBody PushTokenRequest request,
            Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            log.info("🧪 Testing FCM notification for user: {} with token: {}", user.getEmail(), request.getPushToken().substring(0, 20) + "...");
            
            // Test FCM notification specifically
            boolean success = pushNotificationService.testFCMNotification(
                request.getPushToken(),
                "🧪 FCM Test Notification",
                "This is a test FCM notification from your CRM backend!"
            );
            
            if (success) {
                log.info("✅ FCM test notification sent successfully to user: {}", user.getEmail());
                return ResponseEntity.ok(Map.of(
                    "message", "FCM test notification sent successfully",
                    "method", "FCM",
                    "tokenType", "FCM"
                ));
            } else {
                log.error("❌ FCM test notification failed for user: {}", user.getEmail());
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "FCM test notification failed",
                    "method", "FCM",
                    "tokenType", "FCM"
                ));
            }
        } catch (Exception e) {
            log.error("❌ Failed to send FCM test notification: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send FCM test notification: " + e.getMessage()));
        }
    }

    /**
     * Get push notification service status (for debugging)
     */
    @GetMapping("/status")
    public ResponseEntity<?> getServiceStatus(Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            Map<String, Object> status = pushNotificationService.getServiceStatus();
            log.info("📊 Push notification service status requested by user: {}", user.getEmail());
            
            return ResponseEntity.ok(Map.of(
                "status", status,
                "user", user.getEmail(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("❌ Failed to get service status: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get service status: " + e.getMessage()));
        }
    }

    /**
     * Get user's push tokens
     */
    @GetMapping("/tokens")
    public ResponseEntity<?> getUserPushTokens(Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            List<PushToken> tokens = pushTokenService.getActivePushTokensByUser(user);
            
            var tokenData = tokens.stream()
                .map(token -> Map.of(
                    "id", token.getId(),
                    "deviceType", token.getDeviceType(),
                    "isActive", token.getIsActive(),
                    "createdAt", token.getCreatedAt()
                ))
                .toList();
            
            return ResponseEntity.ok(Map.of("tokens", tokenData));
        } catch (Exception e) {
            log.error("❌ Failed to get user push tokens: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get push tokens"));
        }
    }

    /**
     * 🔒 SECURITY: Logout endpoint - deactivate all push tokens for current user
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            // Deactivate all push tokens for this user
            pushTokenService.deactivateAllTokensForUser(user);
            
            log.info("🔇 User logged out: {} - All push tokens deactivated", user.getEmail());
            return ResponseEntity.ok(Map.of(
                "message", "Logout successful - All push tokens deactivated",
                "userEmail", user.getEmail(),
                "timestamp", System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("❌ Failed to logout user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to logout user"));
        }
    }

    /**
     * Deactivate push token
     */
    @DeleteMapping("/tokens/{tokenId}")
    public ResponseEntity<?> deactivatePushToken(
            @PathVariable Long tokenId,
            Authentication authentication) {
        try {
            User user = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            pushTokenService.deactivatePushToken(tokenId);
            
            log.info("🔇 Push token deactivated: {}", tokenId);
            return ResponseEntity.ok(Map.of("message", "Push token deactivated successfully"));
        } catch (Exception e) {
            log.error("❌ Failed to deactivate push token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to deactivate push token"));
        }
    }
}
