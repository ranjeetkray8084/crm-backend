package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.LeadRemark;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.LeadRemarkRepository;
import com.example.real_estate_crm.repository.LeadRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.LeadDao;
import com.example.real_estate_crm.service.dao.UserDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.PageRequest;

@RestController
@RequestMapping("/api/companies/{companyId}/leads")
public class LeadController {

    private final LeadDao leadService;
    private final UserDao userService;
    private final NotificationService notificationService;
    private final LeadRepository leadRepository;
    private final LeadRemarkRepository leadRemarkRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public LeadController(
            LeadDao leadService,
            UserDao userService,
            NotificationService notificationService,
            LeadRepository leadRepository,
            LeadRemarkRepository leadRemarkRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository) {
        this.leadService = leadService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.leadRepository = leadRepository;
        this.leadRemarkRepository = leadRemarkRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
    }

    @GetMapping
    public ResponseEntity<Page<Lead>> getAllLeads(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("leadId").descending());
        Page<Lead> leadsPage = leadRepository.findByCompany_IdAndStatusNot(companyId, Lead.LeadStatus.DROPED, pageable);

        return ResponseEntity.ok(leadsPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Lead> getLead(@PathVariable Long companyId, @PathVariable Long id) {
        Lead lead = leadService.getById(companyId, id);
        return lead == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(lead);
    }

    @PostMapping
    public ResponseEntity<?> addLead(@PathVariable Long companyId, @RequestBody Lead lead) {
        // Validate createdBy.userId
        if (lead.getCreatedBy() == null || lead.getCreatedBy().getUserId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("❌ 'createdBy.userId' is required to create a lead.");
        }

        Long userId = lead.getCreatedBy().getUserId();

        // Fetch user from DB
        Optional<User> optionalUser = userService.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("❌ Invalid user. Please login again.");
        }

        User creator = optionalUser.get();

        // Fetch company from DB
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("❌ Company not found.");
        }

        // Attach user and company
        lead.setCreatedBy(creator);
        lead.setCompany(company);

        try {
            // Save lead
            Lead createdLead = leadService.addLead(companyId, lead);

            // Notification Logic
            try {
                System.out.println("🔔 DEBUG: Starting notification process for lead creation");
                System.out.println("🔔 DEBUG: Creator: " + creator.getName() + " (Role: " + creator.getRole() + ")");
                System.out.println("🔔 DEBUG: Company: " + company.getId());
                System.out.println("🔔 DEBUG: Lead: " + createdLead.getName());
                
                String message = "📢 A new lead \"" + createdLead.getName() + "\" was created by " + creator.getName();
                System.out.println("🔔 DEBUG: Notification message: " + message);

                if (creator.getRole() == User.Role.USER) {
                    System.out.println("🔔 DEBUG: Creator is USER - checking for director and admin notifications");
                    
                    // USER created lead - always notify director first
                    User director = userService.findDirectorByCompany(company);
                    if (director != null) {
                        System.out.println("🔔 DEBUG: Found director: " + director.getName() + " (ID: " + director.getUserId() + ")");
                        System.out.println("🔔 DEBUG: Sending notification to director...");
                        notificationService.sendNotification(director.getUserId(), company, message);
                        System.out.println("🔔 DEBUG: Director notification sent successfully");
                    } else {
                        System.out.println("🔔 DEBUG: No director found for company " + company.getId());
                    }
                    
                    // Also notify admin if user has admin assigned
                    if (creator.getAdmin() != null) {
                        System.out.println("🔔 DEBUG: Creator has admin: " + creator.getAdmin().getName() + " (ID: " + creator.getAdmin().getUserId() + ")");
                        System.out.println("🔔 DEBUG: Sending notification to admin...");
                        notificationService.sendNotification(creator.getAdmin().getUserId(), company, message);
                        System.out.println("🔔 DEBUG: Admin notification sent successfully");
                    } else {
                        System.out.println("🔔 DEBUG: Creator has no admin assigned");
                    }

                } else if (creator.getRole() == User.Role.ADMIN) {
                    System.out.println("🔔 DEBUG: Creator is ADMIN - checking for director notification");
                    
                    // ADMIN created lead - notify director only
                    User director = userService.findDirectorByCompany(company);
                    if (director != null) {
                        System.out.println("🔔 DEBUG: Found director: " + director.getName() + " (ID: " + director.getUserId() + ")");
                        System.out.println("🔔 DEBUG: Sending notification to director...");
                        notificationService.sendNotification(director.getUserId(), company, message);
                        System.out.println("🔔 DEBUG: Director notification sent successfully");
                    } else {
                        System.out.println("🔔 DEBUG: No director found for company " + company.getId());
                    }
                } else if (creator.getRole() == User.Role.DIRECTOR) {
                    System.out.println("🔔 DEBUG: Creator is DIRECTOR - no notifications sent (top level)");
                } else {
                    System.out.println("🔔 DEBUG: Unknown role: " + creator.getRole());
                }
                
                System.out.println("🔔 DEBUG: Lead creation notification process completed successfully");
            } catch (Exception notificationEx) {
                System.err.println("❌ DEBUG: Notification failed for lead creation: " + notificationEx.getMessage());
                notificationEx.printStackTrace();
                // Don't fail the entire operation if notification fails
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(createdLead);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("❌ Failed to create lead: " + ex.getMessage());
        }
    }

    @PutMapping("/{leadId}")
    public ResponseEntity<Lead> updateLeadById(
            @PathVariable Long companyId,
            @PathVariable Long leadId,
            @RequestBody Lead updatedLead) {

        Lead existingLead = leadService.getById(companyId, leadId);
        if (existingLead == null)
            return ResponseEntity.notFound().build();

        // Update fields if not null
        if (updatedLead.getName() != null)
            existingLead.setName(updatedLead.getName());
        if (updatedLead.getEmail() != null) {
            String email = updatedLead.getEmail().trim();
            existingLead.setEmail(email.isEmpty() ? null : email);
        }
        if (updatedLead.getPhone() != null)
            existingLead.setPhone(updatedLead.getPhone());
        if (updatedLead.getSource() != null)
            existingLead.setSource(updatedLead.getSource());
        if (updatedLead.getReferenceName() != null)
            existingLead.setReferenceName(updatedLead.getReferenceName());
        if (updatedLead.getStatus() != null)
            existingLead.setStatus(updatedLead.getStatus());
        if (updatedLead.getAction() != null)
            existingLead.setAction(updatedLead.getAction());
        if (updatedLead.getUpdatedAt() != null)
            existingLead.setUpdatedAt(updatedLead.getUpdatedAt());

        // ✅ Missing fields added
        if (updatedLead.getBudget() != null)
            existingLead.setBudget(updatedLead.getBudget());
        if (updatedLead.getRequirement() != null)
            existingLead.setRequirement(updatedLead.getRequirement());
        if (updatedLead.getLocation() != null)
            existingLead.setLocation(updatedLead.getLocation());

        // Set company
        existingLead.setCompany(new Company(companyId));

        return ResponseEntity.ok(leadService.updateLead(companyId, existingLead));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLead(@PathVariable Long companyId, @PathVariable Long id) {
        try {
            leadService.deleteById(companyId, id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateLeadStatus(
            @PathVariable Long companyId,
            @PathVariable Long id,
            @RequestParam String status) {
        boolean updated = leadService.updateStatus(companyId, id, status);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/source/{source}")
    public ResponseEntity<List<Lead>> getLeadsBySource(
            @PathVariable Long companyId,
            @PathVariable String source) {
        return ResponseEntity.ok(leadService.getLeadsBySource(companyId, source));
    }

    // Controller Method
    @GetMapping("/assigned-to/{userId}")
    public ResponseEntity<Page<Lead>> getLeadsByAssignedUser(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<Lead> leads = leadService.getLeadsByAssignedUserId(companyId, userId, pageable);
        return ResponseEntity.ok(leads);
    }

    @PostMapping("/{leadId}/remarks")
    public ResponseEntity<?> addRemarkToLead(
            @PathVariable Long leadId,
            @RequestBody Map<String, String> requestBody) {

        Optional<Lead> optionalLead = leadRepository.findById(leadId);
        if (optionalLead.isEmpty())
            return ResponseEntity.notFound().build();

        String remarkText = requestBody.get("remark");
        String userIdStr = requestBody.get("userId");

        if (remarkText == null || remarkText.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Remark cannot be empty");
        }

        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body("User ID is required");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid User ID");
        }

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        LeadRemark remark = new LeadRemark();
        remark.setRemark(remarkText);
        remark.setLead(optionalLead.get());
        remark.setCreatedBy(optionalUser.get()); // ✅ Corrected this line

        leadRemarkRepository.save(remark);
        return ResponseEntity.ok("Remark added successfully");
    }

    @GetMapping("/{leadId}/remarks")
    public ResponseEntity<?> getRemarksByLeadId(@PathVariable Long leadId) {
        if (!leadRepository.existsById(leadId))
            return ResponseEntity.notFound().build();

        List<LeadRemark> remarks = leadRemarkRepository.findByLead_LeadId(leadId);
        return ResponseEntity.ok(remarks); // ✅ Returns list of LeadRemark with createdBy summary
    }

    @PutMapping("/{leadId}/assign/{userId}")
    public ResponseEntity<?> assignLead(
            @PathVariable Long companyId,
            @PathVariable Long leadId,
            @PathVariable Long userId,
            @RequestParam Long assignerId 
    ) {
        Optional<User> optionalAssigner = userService.findById(assignerId);
        Optional<User> optionalAssignedUser = userService.findById(userId);

        // 🔍 Validate assigner and assignee existence
        if (optionalAssigner.isEmpty() || optionalAssignedUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        User assigner = optionalAssigner.get();
        User assignedUser = optionalAssignedUser.get();

        // 🔐 Only ADMIN or DIRECTOR can assign
        if (!(assigner.getRole() == User.Role.ADMIN || assigner.getRole() == User.Role.DIRECTOR)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // 🔍 Get the lead to check creator
        Lead lead = leadService.getById(companyId, leadId);
        if (lead == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // 🚫 Prevent self-assignment if user is the creator
        String message = "You cannot assign this lead";
        User creator = lead.getCreatedBy();
        if (creator != null && creator.getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(message); 
        }

        // Assign the lead
        Lead assignedLead = leadService.assignLead(companyId, leadId, userId).orElse(null);
        if (assignedLead == null) {
            return ResponseEntity.badRequest().build();
        }

        // Send notifications
        String assignerRole = assigner.getRole().name(); // ADMIN / DIRECTOR
        String assignerName = assigner.getName();
        String leadName = assignedLead.getName();

        // 📩 Notify assigned user
        String assignedMsg = "📌 Lead \"" + leadName + "\" has been assigned to you by "
                + assignerRole + " " + assignerName;
        notificationService.sendNotification(assignedUser.getUserId(), assignedLead.getCompany(), assignedMsg);

        // 📩 Notify creator if creator is a USER
        User leadCreator = assignedLead.getCreatedBy();
        if (leadCreator != null && leadCreator.getRole() == User.Role.USER) {
            String creatorMsg = "📋 Your lead \"" + leadName + "\" was assigned to "
                    + assignedUser.getName() + " by " + assignerRole + " " + assignerName;
            notificationService.sendNotification(leadCreator.getUserId(), assignedLead.getCompany(), creatorMsg);
        }

        return ResponseEntity.ok(assignedLead);
    }

    @PutMapping("/{leadId}/unassign")
    public ResponseEntity<Lead> unassignLead(
            @PathVariable Long companyId,
            @PathVariable Long leadId,
            @RequestParam Long unassignerId // 👈 Who is unassigning
    ) {
        Optional<User> optionalUnassigner = userService.findById(unassignerId);
        if (optionalUnassigner.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        User unassigner = optionalUnassigner.get();

        // ✅ Check if unassigner is ADMIN or DIRECTOR
        if (!(unassigner.getRole() == User.Role.ADMIN || unassigner.getRole() == User.Role.DIRECTOR)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return leadService.unassignLead(companyId, leadId)
                .map(lead -> {
                    String leadName = lead.getName();
                    String unassignerName = unassigner.getName();
                    String unassignerRole = unassigner.getRole().name();

                    // ✅ Notify previously assigned user
                    if (lead.getAssignedTo() != null) {
                        Long assignedUserId = lead.getAssignedTo().getUserId();
                        String msgToAssignedUser = "❌ Lead \"" + leadName + "\" has been unassigned from you by "
                                + unassignerRole + " " + unassignerName;
                        notificationService.sendNotification(assignedUserId, lead.getCompany(), msgToAssignedUser);
                    }

                    // ✅ Notify creator if USER
                    User creator = lead.getCreatedBy();
                    if (creator != null && creator.getRole() == User.Role.USER) {
                        String msgToCreator = "📋 Lead \"" + leadName + "\" was unassigned by " + unassignerRole + " "
                                + unassignerName;
                        notificationService.sendNotification(creator.getUserId(), lead.getCompany(), msgToCreator);
                    }

                    return ResponseEntity.ok(lead);
                })
                .orElse(ResponseEntity.badRequest().build());
    }

    @GetMapping("/created-by/{userId}")
    public ResponseEntity<Page<Lead>> getLeadsByCreatedBy(
            @PathVariable Long companyId,
            @PathVariable String userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(leadService.getLeadsByCreatedBy(companyId, userId, pageable));
    }

    @GetMapping("/{id}/name")
    public ResponseEntity<String> getLeadNameById(
            @PathVariable Long companyId,
            @PathVariable Long id) {
        return leadRepository.findById(id)
                .map(lead -> lead.getCompany().getId().equals(companyId)
                        ? ResponseEntity.ok(lead.getName())
                        : ResponseEntity.status(403).body("Lead does not belong to this company."))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getTotalLeadCount(@PathVariable Long companyId) {
        long count = leadRepository.countByCompany_Id(companyId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/closed")
    public ResponseEntity<Long> countClosedLeads(@PathVariable Long companyId) {
        // Count both CLOSED and DROPPED leads together
        List<Lead.LeadStatus> statuses = List.of(Lead.LeadStatus.CLOSED, Lead.LeadStatus.DROPED);
        long count = leadRepository.countByCompany_IdAndStatusIn(companyId, statuses);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/created-or-assigned/{userId}")
    public ResponseEntity<Page<Lead>> getLeadsCreatedOrAssignedTo(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Filter out DROPPED leads
        Page<Lead> leads = leadRepository.findByCompany_IdAndStatusNotAndCreatedBy_UserIdOrAssignedTo_UserId(
                companyId,
                Lead.LeadStatus.DROPED, // Status to exclude
                userId,
                userId,
                pageable);

        return ResponseEntity.ok(leads);
    }

    @GetMapping("/count-for-user/{userId}")
    public ResponseEntity<Map<String, Long>> countLeadsForUser(
            @PathVariable Long companyId,
            @PathVariable Long userId) {

        List<Lead> assignedLeads = leadRepository.findByCompanyIdAndAssignedToUserId(companyId, userId);
        List<Lead> createdLeads = leadRepository.findByCompanyIdAndCreatedByUserId(companyId, userId);

        Set<Long> uniqueLeadIds = new HashSet<>();
        long assignedCount = 0;
        long createdCount = 0;
        long closedCount = 0;

        for (Lead lead : assignedLeads) {
            if (uniqueLeadIds.add(lead.getLeadId())) {
                assignedCount++;
                if (lead.getStatus() == Lead.LeadStatus.CLOSED) {
                    closedCount++;
                }
            }
        }

        for (Lead lead : createdLeads) {
            if (uniqueLeadIds.add(lead.getLeadId())) {
                createdCount++;
                if (lead.getStatus() == Lead.LeadStatus.CLOSED) {
                    closedCount++;
                }
            }
        }

        long total = assignedCount + createdCount;

        Map<String, Long> response = new HashMap<>();
        response.put("assignedCount", assignedCount);
        response.put("createdCount", createdCount);
        response.put("total", total);
        response.put("closedCount", closedCount); // 🆕 Closed leads count

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public Page<Lead> getLeads(
            @PathVariable Long companyId,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) Long createdBy,
            @RequestParam(required = false) String source, // ✅ NEW
            @RequestParam(required = false) String action, // ✅ NEW - handles ASSIGNED/UNASSIGNED
            Pageable pageable) {
        // For multiple keywords, use the new method that handles two keywords
        if (keywords != null && keywords.size() >= 2) {
            return leadRepository.searchLeadsWithTwoKeywords(companyId, keywords.get(0), keywords.get(1), status,
                    minBudget, maxBudget, createdBy, source, action, pageable);
        } else if (keywords != null && keywords.size() == 1) {
            // Single keyword - use the original method
            return leadService.searchLeads(companyId, keywords.get(0), status, minBudget, maxBudget, createdBy, source,
                    action, pageable);
        } else {
            // No keywords - use the original method
            return leadService.searchLeads(companyId, null, status, minBudget, maxBudget, createdBy, source, action,
                    pageable);
        }
    }

    @GetMapping("/created-or-assigned/{userId}/search")
    public Page<Lead> searchLeadsCreatedOrAssignedToUser(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        // For multiple keywords, create a search string that matches any of the
        // keywords
        String searchString = null;
        if (keywords != null && !keywords.isEmpty()) {
            // Create a search pattern that matches any of the keywords
            // This will be handled by the SQL LIKE conditions
            searchString = keywords.get(0); // Use first keyword for now
        }

        return leadRepository.searchLeadsCreatedOrAssigned(companyId, userId, searchString, status, minBudget,
                maxBudget, source, action, pageable);
    }

    @GetMapping("/visible-to-admin/{adminId}/search")
    public Page<Lead> searchLeadsVisibleToAdmin(
            @PathVariable Long companyId,
            @PathVariable Long adminId,
            @RequestParam(required = false) Long createdBy, // 👈 Added this
            @RequestParam(required = false) List<String> keywords,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String action,
            Pageable pageable) {
        // For multiple keywords, create a search string that matches any of the
        // keywords
        String searchString = null;
        if (keywords != null && !keywords.isEmpty()) {
            // Create a search pattern that matches any of the keywords
            // This will be handled by the SQL LIKE conditions
            searchString = keywords.get(0); // Use first keyword for now
        }

        return leadRepository.searchLeadsVisibleToAdmin(
                companyId,
                adminId,
                createdBy, // 👈 Pass it here too
                searchString,
                status,
                minBudget,
                maxBudget,
                source,
                action,
                pageable);
    }

    @GetMapping("/admin-visible/{adminId}")
    public ResponseEntity<Page<Lead>> getAllActiveLeadsVisibleToAdmin(
            @PathVariable Long companyId,
            @PathVariable Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created_at").descending());

        // Direct native query call without LeadStatus.DROPED since native uses Strings.
        Page<Lead> leadsPage = leadRepository.getLeadsVisibleToAdmin(companyId, adminId, pageable);

        return ResponseEntity.ok(leadsPage);
    }

    @GetMapping("/count-visible-to-admin/{adminId}")
    public ResponseEntity<Long> countLeadsVisibleToAdmin(@PathVariable Long companyId, @PathVariable Long adminId) {
        long count = leadRepository.countLeadsVisibleToAdmin(companyId, adminId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/count/closed-droped")
    public Long countClosedLeadsByAdmin(
            @PathVariable Long companyId,
            @RequestParam Long adminId) {
        return leadRepository.countClosedLeadsForAdmin(companyId, adminId);
    }

    @GetMapping("/count/summary/{userId}")
    public ResponseEntity<Map<String, Long>> getLeadCountSummary(
            @PathVariable Long companyId,
            @PathVariable Long userId) {

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User user = optionalUser.get();
        User.Role role = user.getRole();
        Map<String, Long> response = new HashMap<>();

        if (role == User.Role.DIRECTOR) {
            // 🔹 DIRECTOR → All leads in the company (excluding DROPPED)
            long total = leadRepository.countByCompany_IdAndStatusNot(companyId, Lead.LeadStatus.DROPED);
            long newLeads = leadRepository.countByCompany_IdAndStatus(companyId, Lead.LeadStatus.NEW);
            long contacted = leadRepository.countByCompany_IdAndStatus(companyId, Lead.LeadStatus.CONTACTED);
            long closed = leadRepository.countByCompany_IdAndStatus(companyId, Lead.LeadStatus.CLOSED);
            long dropped = leadRepository.countByCompany_IdAndStatus(companyId, Lead.LeadStatus.DROPED);

            response.put("totalLeads", total);
            response.put("newLeads", newLeads);
            response.put("contactedLeads", contacted);
            response.put("closedLeads", closed);
            response.put("droppedLeads", dropped);
            response.put("totalClose", closed + dropped); // For deals overview - combined closed and dropped

        } else if (role == User.Role.ADMIN) {
            // 🔹 ADMIN → createdBy admin or assignedTo admin + all assigned users under
            // admin (excluding DROPPED)
            List<Long> userIdsUnderAdmin = userRepository.findByAdmin_UserId(userId)
                    .stream()
                    .map(User::getUserId) // ✅ entity ka correct getter
                    .collect(Collectors.toList());

            userIdsUnderAdmin.add(userId); // include self

            long total = leadRepository.countActiveLeadsByUserIds(companyId, userIdsUnderAdmin);
            long newLeads = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, Lead.LeadStatus.NEW,
                    userIdsUnderAdmin);
            long contacted = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, Lead.LeadStatus.CONTACTED,
                    userIdsUnderAdmin);
            long closed = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, Lead.LeadStatus.CLOSED,
                    userIdsUnderAdmin);
            long dropped = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, Lead.LeadStatus.DROPED,
                    userIdsUnderAdmin);

            response.put("totalLeads", total);
            response.put("newLeads", newLeads);
            response.put("contactedLeads", contacted);
            response.put("closedLeads", closed);
            response.put("droppedLeads", dropped);
            response.put("totalClose", closed + dropped); // For deals overview - combined closed and dropped

        } else if (role == User.Role.USER) {
            // 🔹 USER → leads created by user OR assigned to user (excluding DROPPED)
            long total = leadRepository.countActiveLeadsByCreatedByOrAssignedTo(companyId, userId);
            long newLeads = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId,
                    Lead.LeadStatus.NEW, userId);
            long contacted = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId,
                    Lead.LeadStatus.CONTACTED, userId);
            long closed = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId,
                    Lead.LeadStatus.CLOSED, userId);
            long dropped = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId,
                    Lead.LeadStatus.DROPED, userId);

            response.put("totalLeads", total);
            response.put("newLeads", newLeads);
            response.put("contactedLeads", contacted);
            response.put("closedLeads", closed);
            response.put("droppedLeads", dropped);
            response.put("totalClose", closed + dropped); // For deals overview - combined closed and dropped
        }

        return ResponseEntity.ok(response);
    }

}
