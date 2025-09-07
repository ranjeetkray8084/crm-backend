package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.PushToken;
import com.example.real_estate_crm.repository.PushTokenRepository;
import com.example.real_estate_crm.service.SimplePushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simple-push")
@CrossOrigin(origins = "*")
public class SimplePushTokenController {

    private static final Logger logger = LoggerFactory.getLogger(SimplePushTokenController.class);

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private SimplePushNotificationService simplePushNotificationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerPushToken(@RequestBody Map<String, Object> request) {
        try {
            String pushToken = (String) request.get("pushToken");
            Long userId = Long.valueOf(request.get("userId").toString());

            if (pushToken == null || pushToken.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Push token is required"));
            }

            // Deactivate any existing tokens with the same pushToken string
            pushTokenRepository.deactivateByPushToken(pushToken);

            // Create new push token
            PushToken token = new PushToken();
            token.setPushToken(pushToken);
            token.setUserId(userId);
            token.setIsActive(true);

            pushTokenRepository.save(token);

            logger.info("✅ SIMPLE: Push token registered for user: {}", userId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Push token registered successfully"));

        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error registering push token", e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Error registering push token"));
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

    @GetMapping("/tokens/{userId}")
    public ResponseEntity<List<PushToken>> getUserTokens(@PathVariable Long userId) {
        try {
            List<PushToken> tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
            return ResponseEntity.ok(tokens);
        } catch (Exception e) {
            logger.error("❌ SIMPLE: Error fetching user tokens", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}
