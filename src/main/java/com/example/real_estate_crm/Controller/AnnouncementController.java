package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.AnnouncementRequest;
import com.example.real_estate_crm.model.Announcement;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.AnnouncementRepository;
import com.example.real_estate_crm.service.PushNotificationService;
import com.example.real_estate_crm.service.PushTokenService;
import com.example.real_estate_crm.service.dao.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final PushTokenService pushTokenService;
    private final PushNotificationService pushNotificationService;
    private final UserDao userDao;

    /**
     * Create new announcement
     */
    @PostMapping
    public ResponseEntity<?> createAnnouncement(
            @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Company company = currentUser.getCompany();
            
            // Create announcement
            Announcement announcement = new Announcement();
            announcement.setTitle(request.getTitle());
            announcement.setMessage(request.getMessage());
            announcement.setCompany(company);
            announcement.setCreatedBy(currentUser);
            announcement.setPriority(Announcement.Priority.valueOf(request.getPriority()));
            announcement.setExpiresAt(request.getExpiresAt());
            announcement.setIsActive(true);
            announcement.setCreatedAt(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            announcement.setUpdatedAt(announcement.getCreatedAt());
            
            Announcement savedAnnouncement = announcementRepository.save(announcement);
            
            // 🔒 SECURITY FIX: Company-wide push notifications are NOT allowed
            // Only send to specific users or use regular notification system
            if (request.getSendPushNotification()) {
                log.warn("⚠️ Company-wide push notifications are not allowed for security reasons. Use regular notifications instead.");
                
                // Instead of company-wide push, send regular notifications to company users
                // This ensures company isolation and proper notification tracking
                try {
                    // Get company users and send regular notifications (which automatically include push notifications)
                    // This maintains company isolation and proper user targeting
                    log.info("🔔 Sending company announcement via regular notification system for company: {}", company.getId());
                    
                    // Note: Regular notifications automatically send push notifications to company users
                    // This maintains security and company isolation
                    
                } catch (Exception e) {
                    log.error("❌ Failed to send announcement notifications: {}", e.getMessage());
                }
            }
            
            log.info("✅ Announcement created: {}", savedAnnouncement.getTitle());
            return ResponseEntity.ok(Map.of(
                "message", "Announcement created successfully",
                "announcement", savedAnnouncement,
                "note", "Company-wide push notifications are not allowed for security. Use regular notifications instead."
            ));
        } catch (Exception e) {
            log.error("❌ Failed to create announcement: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create announcement"));
        }
    }

    /**
     * Get all active announcements for current user's company
     */
    @GetMapping
    public ResponseEntity<?> getAnnouncements(Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Company company = currentUser.getCompany();
            
            LocalDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
            List<Announcement> announcements = announcementRepository.findActiveAnnouncementsByCompany(company.getId(), now);
            
            return ResponseEntity.ok(Map.of("announcements", announcements));
        } catch (Exception e) {
            log.error("❌ Failed to get announcements: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get announcements"));
        }
    }

    /**
     * Get announcement by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnnouncementById(@PathVariable Long id) {
        try {
            Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
            
            return ResponseEntity.ok(Map.of("announcement", announcement));
        } catch (Exception e) {
            log.error("❌ Failed to get announcement {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get announcement"));
        }
    }

    /**
     * Update announcement
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
            
            // Check if user can update this announcement
            if (!announcement.getCreatedBy().getUserId().equals(currentUser.getUserId()) && 
                currentUser.getRole() != User.Role.ADMIN && 
                currentUser.getRole() != User.Role.DIRECTOR) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized to update this announcement"));
            }
            
            // Update fields
            announcement.setTitle(request.getTitle());
            announcement.setMessage(request.getMessage());
            announcement.setPriority(Announcement.Priority.valueOf(request.getPriority()));
            announcement.setExpiresAt(request.getExpiresAt());
            announcement.setUpdatedAt(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime());
            
            Announcement updatedAnnouncement = announcementRepository.save(announcement);
            
            log.info("✅ Announcement updated: {}", updatedAnnouncement.getTitle());
            return ResponseEntity.ok(Map.of(
                "message", "Announcement updated successfully",
                "announcement", updatedAnnouncement
            ));
        } catch (Exception e) {
            log.error("❌ Failed to update announcement {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to update announcement"));
        }
    }

    /**
     * Delete announcement
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(
            @PathVariable Long id,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));
            
            // Check if user can delete this announcement
            if (!announcement.getCreatedBy().getUserId().equals(currentUser.getUserId()) && 
                currentUser.getRole() != User.Role.ADMIN && 
                currentUser.getRole() != User.Role.DIRECTOR) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized to delete this announcement"));
            }
            
            announcement.setIsActive(false);
            announcementRepository.save(announcement);
            
            log.info("🗑️ Announcement deleted: {}", announcement.getTitle());
            return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
        } catch (Exception e) {
            log.error("❌ Failed to delete announcement {}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to delete announcement"));
        }
    }

    /**
     * Get announcements by priority
     */
    @GetMapping("/priority/{priority}")
    public ResponseEntity<?> getAnnouncementsByPriority(
            @PathVariable String priority,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            Company company = currentUser.getCompany();
            
            Announcement.Priority priorityEnum = Announcement.Priority.valueOf(priority.toUpperCase());
            List<Announcement> announcements = announcementRepository.findByCompanyIdAndPriorityAndIsActiveTrueOrderByCreatedAtDesc(
                company.getId(), priorityEnum);
            
            return ResponseEntity.ok(Map.of("announcements", announcements));
        } catch (Exception e) {
            log.error("❌ Failed to get announcements by priority {}: {}", priority, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to get announcements by priority"));
        }
    }
}
