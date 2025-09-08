package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.AnnouncementRequest;
import com.example.real_estate_crm.dto.DeveloperAnnouncementRequest;
import com.example.real_estate_crm.model.Announcement;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.AnnouncementRepository;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.service.SimplePushNotificationService;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Slf4j
public class AnnouncementController {

    private final AnnouncementRepository announcementRepository;
    private final CompanyRepository companyRepository;
    private final SimplePushNotificationService simplePushNotificationService;
    private final NotificationService notificationService;
    private final UserDao userDao;

    /**
     * Upload image for announcement
     */
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate developer role
            if (!currentUser.getRole().equals(User.Role.DEVELOPER)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only developers can upload images"));
            }
            
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            
            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/"))) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed"));
            }
            
            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
            String filename = "announcement_" + UUID.randomUUID().toString() + extension;
            
            // Create uploads directory if it doesn't exist
            Path uploadsDir = Paths.get("uploads");
            if (!Files.exists(uploadsDir)) {
                Files.createDirectories(uploadsDir);
            }
            
            // Save file
            Path filePath = uploadsDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath);
            
            // Return relative URL
            String imageUrl = "/uploads/" + filename;
            
            log.info("✅ Image uploaded successfully: {}", imageUrl);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
            
        } catch (IOException e) {
            log.error("❌ Failed to upload image: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to upload image"));
        } catch (Exception e) {
            log.error("❌ Error uploading image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Error uploading image"));
        }
    }

    /**
     * Create new announcement
     */
    @PostMapping
    public ResponseEntity<?> createAnnouncement(
            @RequestBody DeveloperAnnouncementRequest request,
            Authentication authentication) {
        try {
            User currentUser = userDao.findByEmail(authentication.getName()).orElseThrow(() -> new RuntimeException("User not found"));
            
            // Validate developer role
            if (!currentUser.getRole().equals(User.Role.DEVELOPER)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only developers can create announcements"));
            }
            
            // Create announcement
            Announcement announcement = new Announcement();
            
            // Generate title from content (first 60 characters)
            String title = request.getContent().length() > 60 ? 
                request.getContent().substring(0, 60) + "..." : 
                request.getContent();
            announcement.setTitle(title);
            
            announcement.setMessage(request.getContent());
            announcement.setContent(request.getContent());
            announcement.setImageUrl(request.getImageUrl());
            announcement.setCreatedBy(currentUser);
            
            // Set company based on scope
            Company company = null;
            if ("ONE_COMPANY".equals(request.getScope())) {
                company = companyRepository.findById(request.getCompanyId()).orElse(null);
            } else if ("SPECIFIC_COMPANIES".equals(request.getScope()) && request.getCompanyIds() != null && !request.getCompanyIds().isEmpty()) {
                // For multiple companies, we'll create separate announcements
                company = companyRepository.findById(request.getCompanyIds().get(0)).orElse(null);
            } else {
                // ALL_COMPANIES - use current user's company as default
                company = currentUser.getCompany();
            }
            
            if (company == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid company selection"));
            }
            
            announcement.setCompany(company);
            
            // Set 24-hour expiry
            announcement.setExpiresAt(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).plusHours(24).toLocalDateTime());
            announcement.setIsActive(true);
            
            Announcement savedAnnouncement = announcementRepository.save(announcement);
            
            // Send push notifications based on scope and audience
            try {
                String pushMessage = "New Announcement: " + title;
                Map<String, String> pushData = new HashMap<>();
                pushData.put("type", "announcement");
                pushData.put("announcementId", savedAnnouncement.getId().toString());
                
                if ("ALL_COMPANIES".equals(request.getScope())) {
                    // Send to all users in all companies
                    if ("ALL_USERS".equals(request.getAudience())) {
                        // Send to all users
                        simplePushNotificationService.sendNotificationToAllUsers(pushMessage, pushData);
                        
                        // Send in-app notifications to all users
                        List<User> allUsers = userDao.getAllUsers();
                        for (User user : allUsers) {
                            if (user.getStatus().equals("ACTIVE")) {
                                notificationService.sendNotification(user.getUserId(), user.getCompany(), 
                                    "📢 New announcement: " + title);
                            }
                        }
                    } else {
                        // Send to directors only
                        // Get all users and filter directors
                        List<User> allUsers = userDao.getAllUsers();
                        for (User user : allUsers) {
                            if (user.getRole().equals(User.Role.DIRECTOR) && user.getStatus().equals("ACTIVE")) {
                                simplePushNotificationService.sendNotificationToUser(user.getUserId(), pushMessage, pushData);
                                notificationService.sendNotification(user.getUserId(), user.getCompany(), 
                                    "📢 New announcement: " + title);
                            }
                        }
                    }
                } else {
                    // Send to specific company/companies
                    List<Long> targetCompanyIds = new ArrayList<>();
                    if ("ONE_COMPANY".equals(request.getScope())) {
                        targetCompanyIds.add(request.getCompanyId());
                    } else if ("SPECIFIC_COMPANIES".equals(request.getScope())) {
                        targetCompanyIds = request.getCompanyIds();
                    }
                    
                    for (Long companyId : targetCompanyIds) {
                        List<User> companyUsers = userDao.findUsersByCompanyId(companyId);
                        Company targetCompany = companyRepository.findById(companyId).orElse(null);
                        
                        for (User user : companyUsers) {
                            if (user.getStatus().equals("ACTIVE")) {
                                if ("ALL_USERS".equals(request.getAudience()) || 
                                    ("DIRECTOR_ONLY".equals(request.getAudience()) && user.getRole().equals(User.Role.DIRECTOR))) {
                                    
                                    // Send push notification
                                    simplePushNotificationService.sendNotificationToUser(user.getUserId(), pushMessage, pushData);
                                    
                                    // Send in-app notification
                                    if (targetCompany != null) {
                                        notificationService.sendNotification(user.getUserId(), targetCompany, 
                                            "📢 New announcement: " + title);
                                    }
                                }
                            }
                        }
                    }
                }
                
                log.info("🔔 Push notifications sent for announcement: {}", savedAnnouncement.getId());
                    
            } catch (Exception e) {
                log.error("❌ Failed to send announcement notifications: {}", e.getMessage());
            }
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Announcement created successfully",
                "announcement", savedAnnouncement
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
            announcement.setContent(request.getMessage()); // Set content same as message
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

}
