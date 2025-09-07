package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.PushToken;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.PushTokenRepository;
import com.example.real_estate_crm.service.SimplePushNotificationService;
import com.example.real_estate_crm.service.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/push-tokens")
@CrossOrigin(origins = "*")
public class SimplePushTokenController {

    private static final Logger logger = LoggerFactory.getLogger(SimplePushTokenController.class);

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private SimplePushNotificationService simplePushNotificationService;

    @Autowired
    private UserDao userDao;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerPushToken(@RequestBody Map<String, Object> request) {
        try {
            String pushToken = (String) request.get("pushToken");
            Long userId = Long.valueOf(request.get("userId").toString());
            String deviceId = (String) request.get("deviceId");
            String deviceName = (String) request.get("deviceName");
            String platform = (String) request.get("platform");

            if (pushToken == null || pushToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Push token is required"));
            }

            // Find the user
            User user = userDao.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "User not found"));
            }

            // Deactivate any existing tokens with the same pushToken string
            pushTokenRepository.deactivateByPushToken(pushToken);

            // Create new push token
            PushToken token = new PushToken();
            token.setPushToken(pushToken);
            token.setUser(user);
            token.setUserId(userId);
            token.setDeviceId(deviceId);
            token.setDeviceName(deviceName);
            token.setPlatform(platform);
            token.setIsActive(true);

            pushTokenRepository.save(token);

            logger.info("✅ SIMPLE: Push token registered for user: {} device: {}", userId, deviceId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Push token registered successfully"));

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error registering push token", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Error registering push token"));
        }
    }

    @PostMapping("/deactivate")
    public ResponseEntity<Map<String, Object>> deactivatePushToken(@RequestBody Map<String, Object> request) {
        try {
            String pushToken = (String) request.get("pushToken");

            if (pushToken == null || pushToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Push token is required"));
            }

            int updated = pushTokenRepository.deactivateByPushToken(pushToken);
            
            if (updated > 0) {
                logger.info("✅ SIMPLE: Push token deactivated: {}", pushToken);
                return ResponseEntity.ok(Map.of("success", true, "message", "Push token deactivated successfully"));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "No active push token found"));
            }

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error deactivating push token", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error deactivating push token"));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody Map<String, Object> request) {
        try {
            Long userId = Long.valueOf(request.get("userId").toString());
            String message = (String) request.get("message");
            
            if (message == null || message.trim().isEmpty()) {
                message = "Test notification from CRM App!";
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", "test");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            boolean success = simplePushNotificationService.sendNotificationToUser(userId, message, data);

            if (success) {
                logger.info("✅ SIMPLE: Test notification sent to user: {}", userId);
                return ResponseEntity.ok(Map.of("success", true, "message", "Test notification sent successfully"));
            } else {
                logger.warn("⚠️ SIMPLE: Failed to send test notification to user: {}", userId);
                return ResponseEntity.ok(Map.of("success", false, "message", "Failed to send notification - no active tokens found"));
            }

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error sending test notification", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error sending test notification"));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PushToken>> getUserTokens(@PathVariable Long userId) {
        try {
            List<PushToken> tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error fetching user tokens", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @DeleteMapping("/device/{deviceId}")
    public ResponseEntity<Map<String, Object>> deactivateDeviceToken(@PathVariable String deviceId) {
        try {
            int updated = pushTokenRepository.deactivateByDeviceId(deviceId);
            
            if (updated > 0) {
                logger.info("✅ SIMPLE: Device token deactivated: {}", deviceId);
                return ResponseEntity.ok(Map.of("success", true, "message", "Device token deactivated successfully"));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "No active device token found"));
            }

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error deactivating device token", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Error deactivating device token"));
        }
    }

    @PostMapping("/user/{userId}/deactivate-all")
    public ResponseEntity<Map<String, Object>> deactivateAllUserTokens(@PathVariable Long userId) {
        try {
            int updated = pushTokenRepository.deactivateByUserId(userId);
            
            if (updated > 0) {
                logger.info("✅ SIMPLE: All tokens deactivated for user: {}", userId);
                return ResponseEntity.ok(Map.of("success", true, "message", "All user tokens deactivated successfully"));
            } else {
                return ResponseEntity.ok(Map.of("success", false, "message", "No active tokens found for user"));
            }

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error deactivating all user tokens", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Error deactivating all user tokens"));
        }
    }

    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> sendTestNotification(@RequestBody Map<String, Object> request) {
        try {
            String title = (String) request.get("title");
            String message = (String) request.get("message");
            
            if (message == null || message.trim().isEmpty()) {
                message = "Test notification from CRM App!";
            }
            if (title == null || title.trim().isEmpty()) {
                title = "Test Notification";
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", "test");
            data.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // Send to all active users for testing
            boolean success = simplePushNotificationService.sendNotificationToAllUsers(title + ": " + message, data);

            if (success) {
                logger.info("✅ SIMPLE: Test notification sent to all users");
                return ResponseEntity.ok(Map.of("success", true, "message", "Test notification sent successfully"));
            } else {
                logger.warn("⚠️ SIMPLE: Failed to send test notification");
                return ResponseEntity.ok(Map.of("success", false, "message", "Failed to send notification - no active tokens found"));
            }

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error sending test notification", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Error sending test notification"));
        }
    }
}
