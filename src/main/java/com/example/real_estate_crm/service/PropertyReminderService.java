package com.example.real_estate_crm.service;

import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.PropertyRepository;
import com.example.real_estate_crm.service.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PropertyReminderService {

    private static final Logger logger = LoggerFactory.getLogger(PropertyReminderService.class);

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDao userService;

    /**
     * Check for property reminders every hour
     * This will send notifications for properties with reminder dates that have passed
     */
    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 ms)
    public void checkAndSendReminderNotifications() {
        try {
            logger.info("🔔 Starting property reminder check...");
            
            // Get current time
            LocalDateTime now = LocalDateTime.now();
            
            // Find all properties with reminder dates that are due (within the last hour)
            LocalDateTime oneHourAgo = now.minusHours(1);
            List<Property> propertiesWithDueReminders = propertyRepository
                    .findByStatusAndReminderDateBetween(Property.Status.RENT_OUT, oneHourAgo, now);
            
            if (propertiesWithDueReminders.isEmpty()) {
                logger.info("📅 No property reminders due at this time");
                return;
            }
            
            logger.info("📅 Found {} properties with due reminders", propertiesWithDueReminders.size());
            
            for (Property property : propertiesWithDueReminders) {
                try {
                    sendReminderNotification(property);
                    
                    // Clear the reminder date after sending notification to avoid duplicate notifications
                    property.setReminderDate(null);
                    propertyRepository.save(property);
                    
                    logger.info("✅ Reminder notification sent for property: {}", property.getPropertyName());
                    
                } catch (Exception e) {
                    logger.error("❌ Failed to send reminder notification for property: {}", 
                               property.getPropertyName(), e);
                }
            }
            
        } catch (Exception e) {
            logger.error("❌ Error in property reminder check", e);
        }
    }
    
    /**
     * Send reminder notification for a specific property
     */
    private void sendReminderNotification(Property property) {
        String message = String.format(
            "⏰ REMINDER: Property \"%s\" (Rent Out) reminder is due!\n" +
            "📍 Location: %s\n" +
            "💰 Price: %s\n" +
            "📞 Owner Contact: %s\n" +
            "🕐 Reminder was set for: %s",
            property.getPropertyName(),
            property.getLocation() != null ? property.getLocation() : "Not specified",
            property.getPrice() != null ? "₹" + property.getPrice() : "Not specified",
            property.getOwnerContact() != null ? property.getOwnerContact() : "Not specified",
            property.getReminderDate() != null ? 
                property.getReminderDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm")) : 
                "Not specified"
        );
        
        // Send notification to property creator
        if (property.getCreatedBy() != null) {
            notificationService.sendNotification(
                property.getCreatedBy().getUserId(), 
                property.getCompany(), 
                message
            );
        }
        
        // Send notification to director if creator is not director
        if (property.getCreatedBy() != null && property.getCreatedBy().getRole() != User.Role.DIRECTOR) {
            User director = userService.findDirectorByCompany(property.getCompany());
            if (director != null) {
                notificationService.sendNotification(
                    director.getUserId(), 
                    property.getCompany(), 
                    message
                );
            }
        }
        
        // Send notification to admin if creator is a user and has an admin
        if (property.getCreatedBy() != null && 
            property.getCreatedBy().getRole() == User.Role.USER && 
            property.getCreatedBy().getAdmin() != null) {
            
            notificationService.sendNotification(
                property.getCreatedBy().getAdmin().getUserId(), 
                property.getCompany(), 
                message
            );
        }
    }
    
    /**
     * Manual method to check reminders (can be called from API if needed)
     */
    public void checkRemindersManually() {
        logger.info("🔔 Manual reminder check triggered");
        checkAndSendReminderNotifications();
    }
}
