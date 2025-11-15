package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.LeadDao;
import com.example.real_estate_crm.service.dao.PropertyDao;
import com.example.real_estate_crm.service.dao.UserDao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class WebhookController {
    
    private final PropertyDao propertyDao;
    private final LeadDao leadDao;
    private final UserDao userDao;
    private final NotificationService notificationService;
    
    /**
     * Create lead from webhook using external property ID
     */
    @PostMapping("/leads/{source}/{externalPropertyId}")
    public ResponseEntity<Map<String, Object>> createLeadFromWebhook(
            @PathVariable String source,
            @PathVariable String externalPropertyId,
            @RequestBody Map<String, Object> leadData) {
        
        try {
            log.info("Received webhook lead creation request for source: {} and externalPropertyId: {}", 
                    source, externalPropertyId);
            log.info("Received lead data: {}", leadData);
            
            // Find property by external property ID
            Property property = propertyDao.findByExternalPropertyId(externalPropertyId);
            
            if (property == null) {
                log.warn("Property not found for external property ID: {}", externalPropertyId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "Property not found"));
            }
            
            // Get property creator and company
            User propertyCreator = property.getCreatedBy();
            Company company = property.getCompany();
            
            if (propertyCreator == null || company == null) {
                log.warn("Property creator or company not found for property: {}", property.getPropertyName());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Property creator or company not found"));
            }
            
            // Create lead
            Lead lead = createLeadFromWebhookData(leadData, propertyCreator, company, source, property);
            log.info("Lead created before saving - Location: {}, Requirement: {}, Budget: {}, Action: {}", 
                    lead.getLocation(), lead.getRequirement(), lead.getBudget(), lead.getAction());
            
            Lead savedLead = leadDao.addLead(company.getId(), lead);
            log.info("Lead saved after database - Location: {}, Requirement: {}, Budget: {}, Action: {}", 
                    savedLead.getLocation(), savedLead.getRequirement(), savedLead.getBudget(), savedLead.getAction());
            
            // Send notifications
            sendWebhookNotifications(propertyCreator, company, savedLead, property, source);
            
            log.info("Lead created successfully from webhook: Lead ID {}, Property: {}, Creator: {}", 
                    savedLead.getLeadId(), property.getPropertyName(), propertyCreator.getName());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "leadId", savedLead.getLeadId(),
                    "message", "Lead created successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error creating lead from webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
    
    /**
     * Create lead with direct company and user assignment (fallback method)
     */
    @PostMapping("/leads/company/{companyId}/user/{userId}")
    public ResponseEntity<Map<String, Object>> createLeadWithDirectAssignment(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @RequestBody Map<String, Object> leadData) {
        
        try {
            log.info("Received webhook lead creation request for company: {} and user: {}", 
                    companyId, userId);
            
            // Get user and company
            User user = userDao.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "message", "User not found"));
            }
            
            Company company = user.getCompany();
            if (company == null || !company.getId().equals(companyId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "Company mismatch"));
            }
            
            // Get source from leadData, default to "DIRECT_ASSIGNMENT" if not provided
            String source = (String) leadData.get("source");
            if (source == null || source.trim().isEmpty()) {
                source = "DIRECT_ASSIGNMENT";
            }
            
            log.info("Creating lead with source: {}", source);
            
            // Create lead
            Lead lead = createLeadFromWebhookData(leadData, user, company, source, null);
            Lead savedLead = leadDao.addLead(companyId, lead);
            
            // Send notifications
            sendWebhookNotifications(user, company, savedLead, null, source);
            
            log.info("Lead created successfully with direct assignment: Lead ID {}, User: {}, Source: {}", 
                    savedLead.getLeadId(), user.getName(), source);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "leadId", savedLead.getLeadId(),
                    "message", "Lead created successfully"
            ));
            
        } catch (Exception e) {
            log.error("Error creating lead with direct assignment: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Internal server error"));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "message", "Webhook integration is running",
                "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Create lead from webhook data
     */
    private Lead createLeadFromWebhookData(Map<String, Object> leadData, User createdBy, Company company, String source, Property property) {
        Lead lead = new Lead();
        
        // Debug: Print all incoming data
        log.info("Creating lead from webhook data: {}", leadData);
        
        // Set basic information
        lead.setName((String) leadData.get("name"));
        lead.setPhone((String) leadData.get("phone"));
        lead.setEmail((String) leadData.get("email"));
        
        // Set additional fields from webhook data
        String location = (String) leadData.get("location");
        String requirement = (String) leadData.get("requirement");
        String budgetStr = (String) leadData.get("budget");
        
        log.info("Setting location: {}, requirement: {}, budget: {}", location, requirement, budgetStr);
        
        lead.setLocation(location);
        
        // ✅ ADD PROPERTY SIZE TO REQUIREMENT
        if (property != null && property.getSize() != null && !property.getSize().trim().isEmpty()) {
            if (requirement != null && !requirement.trim().isEmpty()) {
                requirement = requirement + " | Property Size: " + property.getSize();
            } else {
                requirement = "Property Size: " + property.getSize();
            }
        }
        lead.setRequirement(requirement);
        
        // Handle budget conversion from String to BigDecimal
        if (budgetStr != null && !budgetStr.trim().isEmpty()) {
            try {
                lead.setBudget(new java.math.BigDecimal(budgetStr));
                log.info("Budget set successfully: {}", lead.getBudget());
            } catch (NumberFormatException e) {
                log.warn("Invalid budget format: {}, setting to null", budgetStr);
                lead.setBudget(null);
            }
        } else {
            lead.setBudget(null);
        }
        
        // Set relationships
        lead.setCreatedBy(createdBy);
        lead.setCompany(company);
        // Don't assign to anyone - leave assignedTo as null
        
        // Set source based on webhook source
        lead.setSource(mapWebhookSourceToLeadSource(source));
        
        // Set status and action
        lead.setStatus(Lead.LeadStatus.NEW);
        lead.setAction(Lead.Action.UNASSIGNED); // Set as unassigned
        
        log.info("Final lead before saving - Location: {}, Requirement: {}, Budget: {}, Action: {}", 
                lead.getLocation(), lead.getRequirement(), lead.getBudget(), lead.getAction());
        
        return lead;
    }
    
    /**
     * Map webhook source to lead source
     */
    private Lead.Source mapWebhookSourceToLeadSource(String webhookSource) {
        if (webhookSource == null) {
            return Lead.Source.REFERENCE;
        }
        
        switch (webhookSource.toUpperCase()) {
            case "FACEBOOK":
                return Lead.Source.FACEBOOK;
            case "INSTAGRAM":
                return Lead.Source.INSTAGRAM;
            case "NINETYNINE_ACRES":
            case "99ACRES":
                return Lead.Source.NINETY_NINE_ACRES;
            case "MAGICBRICKS":
                return Lead.Source.MAGIC_BRICKS;
            case "HOUSING":
                return Lead.Source.REFERENCE; // Using REFERENCE as fallback
            case "MakaAN":
                return Lead.Source.REFERENCE; // Using REFERENCE as fallback
            case "PROP":
                return Lead.Source.REFERENCE; // Using REFERENCE as fallback
            case "DIRECT_ASSIGNMENT":
                // Check if it's actually Facebook/Instagram by checking sourceSystem
                // This is a fallback - should be handled by proper source detection
                return Lead.Source.REFERENCE;
            default:
                // Try to detect Facebook/Instagram from source string
                String upperSource = webhookSource.toUpperCase();
                if (upperSource.contains("FACEBOOK")) {
                    return Lead.Source.FACEBOOK;
                } else if (upperSource.contains("INSTAGRAM")) {
                    return Lead.Source.INSTAGRAM;
                }
                return Lead.Source.REFERENCE; // Using REFERENCE as fallback
        }
    }
    
    /**
     * Send notifications for webhook lead creation
     */
    private void sendWebhookNotifications(User propertyCreator, Company company, Lead lead, Property property, String source) {
        try {
            // 1. Property creator ko notification
            String sourceDisplayName = getSourceDisplayName(source);
            
            // Better message format based on whether property exists or not
            String message;
            String propertyInfo;
            
            if (property != null) {
                // Property-based lead (99acres, MagicBricks, etc.)
                propertyInfo = property.getSize() != null && !property.getSize().trim().isEmpty() ? 
                    String.format("%s (%s)", property.getPropertyName(), property.getSize()) : 
                    property.getPropertyName();
                
                message = String.format(
                    "🎉 New lead for property \"%s\" from %s",
                    propertyInfo,
                    sourceDisplayName
                );
            } else {
                // Direct assignment (Facebook/Instagram/Webhook) - no property
                String leadName = lead.getName() != null && !lead.getName().trim().isEmpty() ? 
                    lead.getName() : "New Lead";
                
                message = String.format(
                    "🎉 New lead \"%s\" received from %s",
                    leadName,
                    sourceDisplayName
                );
                
                propertyInfo = "Webhook Lead"; // For admin/director messages
            }
            
            notificationService.sendNotification(
                propertyCreator.getUserId(), 
                company, 
                message
            );
            
            // 2. Admin ko notification (agar creator user hai)
            if (propertyCreator.getRole() == User.Role.USER && propertyCreator.getAdmin() != null) {
                String adminMessage;
                if (property != null) {
                    adminMessage = String.format(
                        "📊 New lead for %s's property \"%s\" from %s",
                        propertyCreator.getName(),
                        propertyInfo,
                        sourceDisplayName
                    );
                } else {
                    String leadName = lead.getName() != null && !lead.getName().trim().isEmpty() ? 
                        lead.getName() : "New Lead";
                    adminMessage = String.format(
                        "📊 New lead \"%s\" assigned to %s from %s",
                        leadName,
                        propertyCreator.getName(),
                        sourceDisplayName
                    );
                }
                
                notificationService.sendNotification(
                    propertyCreator.getAdmin().getUserId(),
                    company,
                    adminMessage
                );
            }
            
            // 3. Director ko notification
            User director = userDao.findDirectorByCompany(company);
            if (director != null && !director.getUserId().equals(propertyCreator.getUserId())) {
                String directorMessage;
                if (property != null) {
                    directorMessage = String.format(
                        "📈 New lead received in your company!\n" +
                        "🏠 Property: %s\n" +
                        "👨‍💼 Assigned to: %s\n" +
                        "🔗 Source: %s",
                        propertyInfo,
                        propertyCreator.getName(),
                        sourceDisplayName
                    );
                } else {
                    String leadName = lead.getName() != null && !lead.getName().trim().isEmpty() ? 
                        lead.getName() : "New Lead";
                    String leadPhone = lead.getPhone() != null ? lead.getPhone() : "N/A";
                    directorMessage = String.format(
                        "📈 New lead received in your company!\n" +
                        "👤 Lead: %s\n" +
                        "📞 Phone: %s\n" +
                        "👨‍💼 Assigned to: %s\n" +
                        "🔗 Source: %s",
                        leadName,
                        leadPhone,
                        propertyCreator.getName(),
                        sourceDisplayName
                    );
                }
                
                notificationService.sendNotification(
                    director.getUserId(),
                    company,
                    directorMessage
                );
            }
            
            log.info("Notifications sent successfully for lead: {} from source: {}", lead.getLeadId(), source);
            
        } catch (Exception e) {
            log.error("Error sending webhook notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Get display name for source
     */
    private String getSourceDisplayName(String source) {
        if (source == null) return "Unknown";
        
        switch (source.toUpperCase()) {
            case "NINETYNINE_ACRES":
            case "99ACRES":
                return "99acres.com";
            case "MAGICBRICKS":
                return "MagicBricks.com";
            case "HOUSING":
                return "Housing.com";
            case "MakaAN":
                return "Makaan.com";
            case "PROP":
                return "Prop.com";
            case "DIRECT_ASSIGNMENT":
                return "Webhook"; // Better name for direct assignment
            case "FACEBOOK":
                return "Facebook";
            case "INSTAGRAM":
                return "Instagram";
            default:
                // Try to detect Facebook/Instagram from source string
                String upperSource = source.toUpperCase();
                if (upperSource.contains("FACEBOOK")) {
                    return "Facebook";
                } else if (upperSource.contains("INSTAGRAM")) {
                    return "Instagram";
                }
                return source;
        }
    }
}
