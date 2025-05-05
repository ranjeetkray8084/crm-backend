package com.example.real_estate_crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.repository.NotificationRepository;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.LeadDao;
import com.example.real_estate_crm.service.dao.UserDao;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/leads")
public class LeadController {

    private final LeadDao leadService;
    private final UserDao userService;
    private final NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    public LeadController(LeadDao leadService, UserDao userService, NotificationService notificationService) {
        this.leadService = leadService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Lead>> getAllLeads() {
        return ResponseEntity.ok(leadService.getAllLeads());
    }

    @GetMapping("/created-by/{userId}")
    public ResponseEntity<List<Lead>> getLeadsByCreatedBy(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(leadService.getLeadsByCreatedBy(userId.toString()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getLead(@PathVariable Long id) {
        Lead lead = leadService.getById(id);
        return lead != null ? ResponseEntity.ok(lead) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Lead> addLead(@RequestBody Lead lead) {
        Lead createdLead = leadService.addLead(lead);

        // Notify admins if created by a user
        if (lead.getCreatedBy() != null) {
            Long creatorId = Long.valueOf(lead.getCreatedBy());
            userService.findById(creatorId).ifPresent(user -> {
                if ("User".equalsIgnoreCase(user.getRole().name())) {
                    String message = "A new lead \"" + createdLead.getName() + "\" was created by " + user.getName();
                    userService.getAllUsers().stream()
                            .filter(admin -> "Admin".equalsIgnoreCase(admin.getRole().name()))
                            .forEach(admin -> notificationService.sendNotification(admin.getUserId(), message));
                }
            });
        }

        return ResponseEntity.ok(createdLead);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Lead> updateLead(@PathVariable Long id, @RequestBody Lead lead) {
        lead.setLeadId(id);
        Lead updatedLead = leadService.updateLead(lead);

        // Notify admins if updated by a user
        if (lead.getCreatedBy() != null) {
            Long creatorId = Long.valueOf(lead.getCreatedBy());
            userService.findById(creatorId).ifPresent(user -> {
                if ("User".equalsIgnoreCase(user.getRole().name())) {
                    String message = "Lead \"" + updatedLead.getName() + "\" was updated by " + user.getName();
                    userService.getAllUsers().stream()
                            .filter(admin -> "Admin".equalsIgnoreCase(admin.getRole().name()))
                            .forEach(admin -> notificationService.sendNotification(admin.getUserId(), message));
                }
            });
        }

        return ResponseEntity.ok(updatedLead);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(@PathVariable Long id) {
        leadService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{leadId}/assign/{userId}")
    public ResponseEntity<Lead> assignLead(@PathVariable Long leadId, @PathVariable Long userId) {
        return leadService.assignLead(leadId, userId)
                .map(lead -> {
                    String message = "Lead \"" + lead.getName() + "\" has been assigned to you.";
                    notificationService.sendNotification(userId, message);
                    return ResponseEntity.ok(lead);
                })
                .orElse(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{leadId}/unassign")
    public ResponseEntity<Lead> unassignLead(@PathVariable Long leadId) {
        return leadService.unassignLead(leadId)
                .map(lead -> {
                    if (lead.getAssignedTo() != null) {
                        Long userId = lead.getAssignedTo().getUserId();
                        String message = "Lead \"" + lead.getName() + "\" has been unassigned from you.";
                        notificationService.sendNotification(userId, message);
                    }
                    return ResponseEntity.ok(lead);
                })
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/{leadId}/status/{status}")
    public ResponseEntity<List<Lead>> getLeadsByStatus(@PathVariable String status) {
        return ResponseEntity.ok(leadService.getLeadsByStatus(status));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Lead> updateStatus(
            @PathVariable("id") Long id,
            @RequestParam("status") Lead.LeadStatus status) {

        Optional<Lead> updated = leadService.updateLeadStatus(id, status);
        return updated.map(ResponseEntity::ok)
                      .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<Lead>> getLeadsBySource(@PathVariable String source) {
        return ResponseEntity.ok(leadService.getLeadsBySource(source));
    }

    @GetMapping("/assigned-to/{userId}")
    public ResponseEntity<List<Lead>> getLeadsByAssignedUser(@PathVariable Long userId) {
        return ResponseEntity.ok(leadService.getLeadsByAssignedUserId(userId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Lead>> searchLeadsByName(@RequestParam String name) {
        return ResponseEntity.ok(leadService.searchLeadsByName(name));
    }
}
