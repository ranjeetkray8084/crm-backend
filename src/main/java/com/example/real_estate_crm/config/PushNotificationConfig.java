package com.example.real_estate_crm.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import jakarta.annotation.PostConstruct;

@Configuration
@PropertySource("classpath:application.properties")
@Slf4j
public class PushNotificationConfig {

    @Value("${firebase.project.id:}")
    private String firebaseProjectId;
    
    @Value("${firebase.service.account.path:}")
    private String serviceAccountPath;

    @PostConstruct
    public void init() {
        log.info("🔧 Push Notification Configuration Initialized (Admin SDK)");
        
        // Check Firebase Project ID
        if (firebaseProjectId == null || firebaseProjectId.trim().isEmpty()) {
            log.error("❌ FIREBASE_PROJECT_ID is not configured!");
            log.error("❌ Please set firebase.project.id in application.properties");
        } else {
            log.info("✅ Firebase Project ID: {}", firebaseProjectId);
        }
        
        // Check Service Account Path
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            log.error("❌ FIREBASE_SERVICE_ACCOUNT_PATH is not configured!");
            log.error("❌ Please set firebase.service.account.path in application.properties");
        } else {
            log.info("✅ Firebase Service Account Path: {}", serviceAccountPath);
        }
        
        // Overall status
        boolean allConfigured = (firebaseProjectId != null && !firebaseProjectId.trim().isEmpty()) &&
                               (serviceAccountPath != null && !serviceAccountPath.trim().isEmpty());
        
        if (allConfigured) {
            log.info("🎉 Firebase Admin SDK Push Notifications are fully configured and ready to use!");
            log.info("🎉 Using modern Firebase Admin SDK instead of deprecated Legacy API");
        } else {
            log.error("🚨 Firebase Admin SDK Push Notifications are NOT properly configured!");
            log.error("🚨 Push notifications will not work until all required properties are set");
        }
    }
}
