package com.example.real_estate_crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PushNotificationService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    
    @Value("${firebase.project.id:}")
    private String firebaseProjectId;
    
    @Value("${firebase.service.account.path:}")
    private String serviceAccountPath;
    
    private FirebaseApp firebaseApp;
    private FirebaseMessaging firebaseMessaging;
    private final ObjectMapper objectMapper;

    public PushNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initialize() {
        try {
            log.info("🔧 Initializing Firebase Admin SDK...");
            
            if (firebaseProjectId == null || firebaseProjectId.trim().isEmpty()) {
                log.error("❌ Firebase Project ID not configured");
                return;
            }
            
            if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
                log.error("❌ Firebase Service Account path not configured");
                return;
            }

            // Load service account credentials
            InputStream serviceAccountStream = new ClassPathResource(serviceAccountPath.replace("classpath:", "")).getInputStream();
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccountStream);
            
            // Initialize Firebase Admin SDK
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setProjectId(firebaseProjectId)
                .build();
            
            firebaseApp = FirebaseApp.initializeApp(options);
            firebaseMessaging = FirebaseMessaging.getInstance(firebaseApp);
            
            log.info("✅ Firebase Admin SDK initialized successfully for project: {}", firebaseProjectId);
            
        } catch (IOException e) {
            log.error("❌ Failed to initialize Firebase Admin SDK: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("❌ Unexpected error initializing Firebase Admin SDK: {}", e.getMessage(), e);
        }
    }

    /**
     * Send push notification to a single device
     */
    public boolean sendPushNotification(String pushToken, String title, String body, Map<String, Object> data) {
        try {
            // Detect token type and send accordingly
            if (isFCMToken(pushToken)) {
                return sendFCMNotification(pushToken, title, body, data);
            } else {
                return sendExpoNotification(pushToken, title, body, data);
            }
        } catch (Exception e) {
            log.error("❌ Error sending push notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send push notification to multiple devices
     */
    public boolean sendPushNotificationToMultiple(List<String> pushTokens, String title, String body, Map<String, Object> data) {
        if (pushTokens == null || pushTokens.isEmpty()) {
            log.warn("⚠️ No push tokens provided for notification");
            return false;
        }

        boolean allSuccess = true;
        for (String token : pushTokens) {
            boolean success = sendPushNotification(token, title, body, data);
            if (!success) {
                allSuccess = false;
                log.warn("⚠️ Failed to send notification to token: {}", token.substring(0, Math.min(20, token.length())) + "...");
            }
        }

        log.info("📱 Push notification sent to {} devices, success: {}", pushTokens.size(), allSuccess);
        return allSuccess;
    }

    /**
     * Detect if token is FCM token
     */
    private boolean isFCMToken(String token) {
        if (token == null) return false;
        
        // FCM tokens typically start with specific patterns or are longer
        return token.startsWith("d_") || // Expo FCM tokens
               token.startsWith("fMEP") || // Firebase FCM tokens
               token.startsWith("cpKFluBIRYi") || // Known working FCM token pattern
               token.contains(":APA91b") || // FCM token pattern
               token.length() > 100; // FCM tokens are usually longer
    }

    /**
     * Send FCM notification using Firebase Admin SDK
     */
    private boolean sendFCMNotification(String pushToken, String title, String body, Map<String, Object> data) {
        try {
            if (firebaseMessaging == null) {
                log.error("❌ Firebase Admin SDK not initialized");
                return false;
            }
            
            log.info("🔥 Sending FCM notification to token: {}...", pushToken.substring(0, Math.min(20, pushToken.length())));
            
            // Create FCM message using Admin SDK
            Message.Builder messageBuilder = Message.builder()
                .setToken(pushToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());
            
            // Add data if provided
            if (data != null && !data.isEmpty()) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    messageBuilder.putData(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
            
            Message message = messageBuilder.build();
            
            log.info("📋 FCM message created for token: {}...", pushToken.substring(0, Math.min(20, pushToken.length())));
            
            // Send message using Firebase Admin SDK
            String response = firebaseMessaging.send(message);
            
            log.info("✅ FCM notification sent successfully. Message ID: {}", response);
            return true;
            
        } catch (FirebaseMessagingException e) {
            log.error("❌ FCM messaging error: {}", e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("❌ Error sending FCM notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Send Expo notification (fallback for non-FCM tokens)
     */
    private boolean sendExpoNotification(String pushToken, String title, String body, Map<String, Object> data) {
        try {
            log.info("📱 Sending Expo notification to token: {}...", pushToken.substring(0, Math.min(20, pushToken.length())));
            
            // Create Expo push notification payload
            Map<String, Object> expoPayload = Map.of(
                "to", pushToken,
                "title", title,
                "body", body,
                "data", data != null ? data : Map.of(),
                "sound", "default",
                "badge", 1
            );
            
            // Send to Expo Push Service
            String response = sendHttpRequest(EXPO_PUSH_URL, expoPayload);
            
            if (response != null && response.contains("\"status\":\"ok\"")) {
                log.info("✅ Expo notification sent successfully");
                return true;
            } else {
                log.error("❌ Expo notification failed: {}", response);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ Error sending Expo notification: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send HTTP request to Expo Push Service
     */
    private String sendHttpRequest(String url, Map<String, Object> payload) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
            
            java.net.http.HttpResponse<String> response = client.send(request, 
                java.net.http.HttpResponse.BodyHandlers.ofString());
            
            log.info("📱 Expo Push Service response: {}", response.body());
            return response.body();
            
        } catch (Exception e) {
            log.error("❌ HTTP request failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Send announcement as push notification to all users in company
     */
    public boolean sendAnnouncementPushNotification(String title, String message, List<String> pushTokens) {
        Map<String, Object> data = Map.of(
            "type", "announcement",
            "action", "open_announcements"
        );
        
        return sendPushNotificationToMultiple(pushTokens, title, message, data);
    }

    /**
     * Send lead update notification
     */
    public boolean sendLeadUpdateNotification(String leadName, String action, String pushToken) {
        String title = "Lead Update";
        String body = String.format("Lead '%s' has been %s", leadName, action);
        
        Map<String, Object> data = Map.of(
            "type", "lead_update",
            "action", "open_leads",
            "leadName", leadName
        );
        
        return sendPushNotification(pushToken, title, body, data);
    }

    /**
     * Send task reminder notification
     */
    public boolean sendTaskReminderNotification(String taskName, String dueDate, String pushToken) {
        String title = "Task Reminder";
        String body = String.format("Task '%s' is due on %s", taskName, dueDate);
        
        Map<String, Object> data = Map.of(
            "type", "task_reminder",
            "action", "open_tasks",
            "taskName", taskName,
            "dueDate", dueDate
        );
        
        return sendPushNotification(pushToken, title, body, data);
    }

    /**
     * Send property update notification
     */
    public boolean sendPropertyUpdateNotification(String propertyName, String update, String pushToken) {
        String title = "Property Update";
        String body = String.format("Property '%s' has been %s", propertyName, update);
        
        Map<String, Object> data = Map.of(
            "type", "property_update",
            "action", "open_properties",
            "propertyName", propertyName
        );
        
        return sendPushNotification(pushToken, title, body, data);
    }

    /**
     * Test FCM notification (for debugging)
     */
    public boolean testFCMNotification(String pushToken, String title, String body) {
        log.info("🧪 Testing FCM notification with token: {}", pushToken.substring(0, Math.min(20, pushToken.length())) + "...");
        
        // Check if Firebase Admin SDK is initialized
        if (firebaseMessaging == null) {
            log.error("❌ Firebase Admin SDK not initialized. Please check service account configuration.");
            return false;
        }
        
        // Check if token looks like FCM token
        if (!isFCMToken(pushToken)) {
            log.warn("⚠️ Token doesn't look like FCM token: {}", pushToken.substring(0, Math.min(20, pushToken.length())) + "...");
        }
        
        return sendFCMNotification(pushToken, title, body, Map.of("type", "test", "timestamp", System.currentTimeMillis()));
    }

    /**
     * Get service status for debugging
     */
    public Map<String, Object> getServiceStatus() {
        return Map.of(
            "serviceName", "PushNotificationService (Admin SDK)",
            "firebaseProjectIdConfigured", firebaseProjectId != null && !firebaseProjectId.trim().isEmpty(),
            "firebaseProjectId", firebaseProjectId != null ? firebaseProjectId : "Not Set",
            "serviceAccountPathConfigured", serviceAccountPath != null && !serviceAccountPath.trim().isEmpty(),
            "serviceAccountPath", serviceAccountPath != null ? serviceAccountPath : "Not Set",
            "firebaseAdminSDKInitialized", firebaseMessaging != null,
            "expoPushUrl", EXPO_PUSH_URL,
            "timestamp", System.currentTimeMillis()
        );
    }
}
