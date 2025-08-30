package com.example.real_estate_crm.service;

import com.example.real_estate_crm.model.PushToken;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    /**
     * Register or update push token for a user
     */
    public PushToken registerPushToken(User user, String pushToken, String deviceType) {
        try {
            // Check if token already exists for this user and device
            Optional<PushToken> existingToken = pushTokenRepository.findByUserAndDeviceType(user, deviceType);
            
            if (existingToken.isPresent()) {
                // Update existing token
                PushToken token = existingToken.get();
                token.setPushToken(pushToken);
                token.setIsActive(true);
                log.info("🔄 Updated push token for user: {}", user.getEmail());
                return pushTokenRepository.save(token);
            } else {
                // Create new token
                PushToken newToken = new PushToken();
                newToken.setUser(user);
                newToken.setPushToken(pushToken);
                newToken.setDeviceType(deviceType);
                newToken.setIsActive(true);
                log.info("✅ Registered new push token for user: {}", user.getEmail());
                return pushTokenRepository.save(newToken);
            }
        } catch (Exception e) {
            log.error("❌ Failed to register push token for user {}: {}", user.getEmail(), e.getMessage(), e);
            throw new RuntimeException("Failed to register push token", e);
        }
    }

    /**
     * Get all active push tokens for a user
     */
    public List<PushToken> getActivePushTokensByUser(User user) {
        return pushTokenRepository.findByUserAndIsActiveTrue(user);
    }

    /**
     * 🔒 SECURITY: Company-wide push tokens are NOT allowed
     * This method has been removed to prevent cross-company notifications
     * Use getActivePushTokensByUser() for user-specific notifications instead
     */
    // public List<String> getActivePushTokensByCompany(Long companyId) {
    //     // REMOVED: Company-wide push notifications are not allowed for security
    //     // This prevents notifications from going to wrong companies
    // }

    /**
     * Get push tokens for specific users
     */
    public List<String> getPushTokensByUserIds(List<Long> userIds) {
        List<PushToken> tokens = pushTokenRepository.findByUserIdInAndIsActiveTrue(userIds);
        return tokens.stream()
                .map(PushToken::getPushToken)
                .toList();
    }

    /**
     * Deactivate push token
     */
    public void deactivatePushToken(Long tokenId) {
        try {
            Optional<PushToken> token = pushTokenRepository.findById(tokenId);
            if (token.isPresent()) {
                PushToken pushToken = token.get();
                pushToken.setIsActive(false);
                pushTokenRepository.save(pushToken);
                log.info("🔇 Deactivated push token: {}", tokenId);
            }
        } catch (Exception e) {
            log.error("❌ Failed to deactivate push token {}: {}", tokenId, e.getMessage(), e);
        }
    }

    /**
     * Deactivate all tokens for a user
     */
    public void deactivateAllTokensForUser(User user) {
        try {
            List<PushToken> tokens = pushTokenRepository.findByUserAndIsActiveTrue(user);
            for (PushToken token : tokens) {
                token.setIsActive(false);
            }
            pushTokenRepository.saveAll(tokens);
            log.info("🔇 Deactivated all push tokens for user: {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to deactivate tokens for user {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }

    /**
     * 🔒 SECURITY: Force deactivate all tokens for a user (emergency)
     */
    public void emergencyDeactivateAllTokensForUser(Long userId) {
        try {
            log.warn("🚨 EMERGENCY: Force deactivating all push tokens for user ID: {}", userId);
            
            // Find and deactivate all tokens for this user ID
            List<PushToken> tokens = pushTokenRepository.findByUserUserIdAndIsActiveTrue(userId);
            for (PushToken token : tokens) {
                token.setIsActive(false);
                log.warn("🚨 EMERGENCY: Deactivating push token {} for user ID: {}", token.getId(), userId);
            }
            pushTokenRepository.saveAll(tokens);
            
            log.warn("🚨 EMERGENCY: All {} push tokens deactivated for user ID: {}", tokens.size(), userId);
        } catch (Exception e) {
            log.error("❌ EMERGENCY: Failed to deactivate tokens for user ID {}: {}", userId, e.getMessage(), e);
        }
    }

    /**
     * Clean up old inactive tokens
     */
    public void cleanupOldTokens() {
        try {
            // This would be called by a scheduled task
            log.info("🧹 Cleaning up old push tokens...");
            // Implementation for cleanup logic
        } catch (Exception e) {
            log.error("❌ Failed to cleanup old tokens: {}", e.getMessage(), e);
        }
    }
}
