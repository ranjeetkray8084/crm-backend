package com.example.real_estate_crm.service;

import com.example.real_estate_crm.model.PushToken;
import com.example.real_estate_crm.repository.PushTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimplePushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SimplePushNotificationService.class);

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean sendSinglePushNotification(String pushToken, String message, Map<String, String> data) {
        if (pushToken == null || pushToken.trim().isEmpty()) {
            logger.warn("⚠️ SIMPLE: Push token is null or empty");
            return false;
        }

        boolean isFCM = pushToken.startsWith("ExponentPushToken[") == false;
        logger.info("🚀 SIMPLE: Sending push notification to {} token: {}", isFCM ? "FCM" : "Expo", pushToken.substring(0, Math.min(20, pushToken.length())) + "...");

        if (isFCM) {
            try {
                // FCM message building
                Map<String, Object> fcmMessage = new HashMap<>();
                fcmMessage.put("to", pushToken);
                
                Map<String, Object> notification = new HashMap<>();
                notification.put("body", message);
                fcmMessage.put("notification", notification);
                
                if (data != null && !data.isEmpty()) {
                    fcmMessage.put("data", data);
                }

                // Send FCM message
                String resp = com.google.firebase.messaging.FirebaseMessaging.getInstance().send(
                    com.google.firebase.messaging.Message.builder()
                        .setToken(pushToken)
                        .putData("body", message)
                        .putAllData(data != null ? data : new HashMap<>())
                        .build()
                );
                logger.debug("✅ SIMPLE: FCM sent: {}", resp);
                return true;
            } catch (com.google.firebase.messaging.FirebaseMessagingException fe) {
                logger.error("❌ SIMPLE: FCM send failed, fallback to Expo", fe);
                if ("UNREGISTERED".equals(fe.getMessagingErrorCode().name())) {
                    logger.warn("⚠️ SIMPLE: FCM token is UNREGISTERED, deactivating token: {}", pushToken);
                    pushTokenRepository.deactivateByPushToken(pushToken);
                }
                // No fallback to Expo for FCM tokens if FCM fails
                return false;
            } catch (Exception fe) {
                logger.error("❌ SIMPLE: FCM send failed, fallback to Expo", fe);
                // Fallback to Expo for other FCM errors
            }
        }

        // Expo push notification
        try {
            Map<String, Object> expoMessage = new HashMap<>();
            expoMessage.put("to", pushToken);
            expoMessage.put("sound", "default");
            expoMessage.put("body", message);
            
            if (data != null && !data.isEmpty()) {
                expoMessage.put("data", data);
            }

            String expoUrl = "https://exp.host/--/api/v2/push/send";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(expoMessage), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(expoUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.debug("✅ SIMPLE: Expo sent successfully: {}", response.getBody());
                return true;
            } else {
                logger.error("❌ SIMPLE: Expo send failed with status: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("❌ SIMPLE: Expo send failed", e);
            return false;
        }
    }

    public boolean sendNotificationToUser(Long userId, String message, Map<String, String> data) {
        List<PushToken> tokens = pushTokenRepository.findByUserIdAndIsActiveTrue(userId);
        if (tokens.isEmpty()) {
            logger.warn("⚠️ SIMPLE: No active push tokens found for user: {}", userId);
            return false;
        }

        boolean success = false;
        for (PushToken token : tokens) {
            if (sendSinglePushNotification(token.getPushToken(), message, data)) {
                success = true;
            }
        }
        return success;
    }

    public boolean sendNotificationToAllUsers(String message, Map<String, String> data) {
        List<PushToken> tokens = pushTokenRepository.findByIsActiveTrue();
        if (tokens.isEmpty()) {
            logger.warn("⚠️ SIMPLE: No active push tokens found");
            return false;
        }

        boolean success = false;
        for (PushToken token : tokens) {
            if (sendSinglePushNotification(token.getPushToken(), message, data)) {
                success = true;
            }
        }
        return success;
    }
}
