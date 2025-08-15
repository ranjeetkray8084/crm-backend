package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.LeadRepository;
import com.example.real_estate_crm.repository.PropertyRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.LeadDao;
import com.example.real_estate_crm.service.dao.PropertyDao;
import com.example.real_estate_crm.service.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final LeadRepository leadRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final LeadDao leadDao;
    private final PropertyDao propertyDao;
    private final UserDao userDao;

    @Autowired
    public DashboardController(
            LeadRepository leadRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            LeadDao leadDao,
            PropertyDao propertyDao,
            UserDao userDao) {
        this.leadRepository = leadRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.leadDao = leadDao;
        this.propertyDao = propertyDao;
        this.userDao = userDao;
    }

    /**
     * Get comprehensive dashboard data for a user in a single API call
     * This significantly improves dashboard loading performance
     */
    @GetMapping("/{companyId}/user/{userId}")
    public ResponseEntity<Map<String, Object>> getDashboardData(
            @PathVariable Long companyId,
            @PathVariable Long userId) {
        
        try {
            // Get user details to determine role
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }

            User user = userOptional.get();
            User.Role role = user.getRole();

            Map<String, Object> dashboardData = new HashMap<>();

            // Add user info
            dashboardData.put("user", Map.of(
                "id", user.getUserId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "role", role.toString()
            ));

            // Handle DEVELOPER role (global stats)
            if (role == User.Role.DIRECTOR && companyId == null) {
                dashboardData.put("globalStats", getGlobalStats());
                return ResponseEntity.ok(dashboardData);
            }

            // Company-specific data
            if (companyId != null) {
                // Get leads data based on user role
                Map<String, Object> leadsData = getLeadsData(companyId, userId, role);
                dashboardData.put("leads", leadsData);

                // Get properties data based on user role
                Map<String, Object> propertiesData = getPropertiesData(companyId, userId, role);
                dashboardData.put("properties", propertiesData);

                // Get users and admins overview
                Map<String, Object> usersData = getUsersData(companyId, userId, role);
                dashboardData.put("users", usersData);

                // Get notes data
                Map<String, Object> notesData = getNotesData(companyId, userId, role);
                dashboardData.put("notes", notesData);

                // Get today's follow-ups
                Map<String, Object> followUpsData = getTodayFollowUpsData(companyId, userId, role);
                dashboardData.put("followUps", followUpsData);

                // Get today's events
                Map<String, Object> eventsData = getTodayEventsData(companyId, userId, role);
                dashboardData.put("events", eventsData);
            }

            return ResponseEntity.ok(dashboardData);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to load dashboard data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Map<String, Object> getGlobalStats() {
        Map<String, Object> globalStats = new HashMap<>();
        
        try {
            long totalCompanies = companyRepository.count();
            long totalUsers = userRepository.count();
            long totalAdmins = userRepository.countByRole(User.Role.ADMIN);
            long totalDirectors = userRepository.countByRole(User.Role.DIRECTOR);

            globalStats.put("totalCompanies", totalCompanies);
            globalStats.put("totalUsers", totalUsers);
            globalStats.put("totalAdmins", totalAdmins);
            globalStats.put("totalDirectors", totalDirectors);
        } catch (Exception e) {
            globalStats.put("error", "Failed to load global stats: " + e.getMessage());
        }

        return globalStats;
    }

    private Map<String, Object> getLeadsData(Long companyId, Long userId, User.Role role) {
        Map<String, Object> leadsData = new HashMap<>();
        
        try {
            if (role == User.Role.DIRECTOR) {
                // Director sees all leads in company
                long totalLeads = leadRepository.countByCompany_IdAndStatusNot(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.DROPED);
                long newLeads = leadRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.NEW);
                long contactedLeads = leadRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.CONTACTED);
                long closedLeads = leadRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.CLOSED);
                long droppedLeads = leadRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.DROPED);

                leadsData.put("totalLeads", totalLeads);
                leadsData.put("newLeads", newLeads);
                leadsData.put("contactedLeads", contactedLeads);
                leadsData.put("closedLeads", closedLeads);
                leadsData.put("droppedLeads", droppedLeads);
                leadsData.put("totalClose", closedLeads + droppedLeads);

            } else if (role == User.Role.ADMIN) {
                // Admin sees leads created by admin or assigned to admin + all assigned users under admin
                List<Long> userIdsUnderAdmin = userRepository.findByAdmin_UserId(userId)
                        .stream()
                        .map(User::getUserId)
                        .toList();
                userIdsUnderAdmin.add(userId); // include self

                long totalLeads = leadRepository.countActiveLeadsByUserIds(companyId, userIdsUnderAdmin);
                long newLeads = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.NEW, userIdsUnderAdmin);
                long contactedLeads = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.CONTACTED, userIdsUnderAdmin);
                long closedLeads = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.CLOSED, userIdsUnderAdmin);
                long droppedLeads = leadRepository.countActiveLeadsByStatusAndUserIds(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.DROPED, userIdsUnderAdmin);

                leadsData.put("totalLeads", totalLeads);
                leadsData.put("newLeads", newLeads);
                leadsData.put("contactedLeads", contactedLeads);
                leadsData.put("closedLeads", closedLeads);
                leadsData.put("droppedLeads", droppedLeads);
                leadsData.put("totalClose", closedLeads + droppedLeads);

            } else if (role == User.Role.USER) {
                // User sees leads created by user OR assigned to user
                long totalLeads = leadRepository.countActiveLeadsByCreatedByOrAssignedTo(companyId, userId);
                long newLeads = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.NEW, userId);
                long contactedLeads = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.CONTACTED, userId);
                long closedLeads = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.CLOSED, userId);
                long droppedLeads = leadRepository.countActiveLeadsByStatusAndCreatedByOrAssignedTo(companyId, com.example.real_estate_crm.model.Lead.LeadStatus.DROPED, userId);

                leadsData.put("totalLeads", totalLeads);
                leadsData.put("newLeads", newLeads);
                leadsData.put("contactedLeads", contactedLeads);
                leadsData.put("closedLeads", closedLeads);
                leadsData.put("droppedLeads", droppedLeads);
                leadsData.put("totalClose", closedLeads + droppedLeads);
            }
        } catch (Exception e) {
            leadsData.put("error", "Failed to load leads data: " + e.getMessage());
        }

        return leadsData;
    }

    private Map<String, Object> getPropertiesData(Long companyId, Long userId, User.Role role) {
        Map<String, Object> propertiesData = new HashMap<>();
        
        try {
            if (role == User.Role.USER) {
                // User sees only their properties
                long totalProperties = propertyRepository.countByCompanyIdAndCreatedByUserId(companyId, userId);
                propertiesData.put("totalProperties", totalProperties);
            } else {
                // Director and Admin see all properties
                long totalProperties = propertyRepository.countByCompany_Id(companyId);
                propertiesData.put("totalProperties", totalProperties);
            }

            // Property overview (available for sale, rent, sold out, rent out)
            try {
                // Get property counts by status for overview
                long availableForSale = propertyRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Property.Status.AVAILABLE_FOR_SALE);
                long availableForRent = propertyRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Property.Status.AVAILABLE_FOR_RENT);
                long soldOut = propertyRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Property.Status.SOLD_OUT);
                long rentOut = propertyRepository.countByCompany_IdAndStatus(companyId, com.example.real_estate_crm.model.Property.Status.RENT_OUT);
                
                Map<String, Long> propertyOverview = Map.of(
                    "available for sale", availableForSale,
                    "available for rent", availableForRent,
                    "sold out", soldOut,
                    "rent out", rentOut
                );
                propertiesData.put("overview", propertyOverview);
            } catch (Exception e) {
                propertiesData.put("overview", Map.of(
                    "available for sale", 0L,
                    "available for rent", 0L,
                    "sold out", 0L,
                    "rent out", 0L
                ));
            }

        } catch (Exception e) {
            propertiesData.put("error", "Failed to load properties data: " + e.getMessage());
        }

        return propertiesData;
    }

    private Map<String, Object> getUsersData(Long companyId, Long userId, User.Role role) {
        Map<String, Object> usersData = new HashMap<>();
        
        try {
            if (role == User.Role.DIRECTOR) {
                long totalUsers = userRepository.countByCompany_Id(companyId);
                long totalDirectors = userRepository.countByCompany_IdAndRole(companyId, User.Role.DIRECTOR);
                totalUsers = totalUsers - totalDirectors; // Exclude directors from user count

                long totalAdmins = userRepository.countByCompany_IdAndRole(companyId, User.Role.ADMIN);
                long activeAdmins = userRepository.countByCompany_IdAndRoleAndStatusTrue(companyId, User.Role.ADMIN);
                long deactiveAdmins = userRepository.countByCompany_IdAndRoleAndStatusFalse(companyId, User.Role.ADMIN);

                long totalNormalUsers = userRepository.countByCompany_IdAndRole(companyId, User.Role.USER);
                long activeNormalUsers = userRepository.countByCompany_IdAndRoleAndStatusTrue(companyId, User.Role.USER);
                long deactiveNormalUsers = userRepository.countByCompany_IdAndRoleAndStatusFalse(companyId, User.Role.USER);

                usersData.put("totalUsers", totalUsers);
                usersData.put("totalAdmins", totalAdmins);
                usersData.put("activeAdmins", activeAdmins);
                usersData.put("deactiveAdmins", deactiveAdmins);
                usersData.put("totalNormalUsers", totalNormalUsers);
                usersData.put("activeNormalUsers", activeNormalUsers);
                usersData.put("deactiveNormalUsers", deactiveNormalUsers);

            } else if (role == User.Role.ADMIN) {
                long totalAssignedUsers = userRepository.countUsersByAdminAndCompany(userId, companyId);
                long activeAssignedUsers = userRepository.countUsersByAdminAndCompanyAndStatus(userId, companyId, true);
                long deactiveAssignedUsers = userRepository.countUsersByAdminAndCompanyAndStatus(userId, companyId, false);

                usersData.put("totalAssignedUsers", totalAssignedUsers);
                usersData.put("activeAssignedUsers", activeAssignedUsers);
                usersData.put("deactiveAssignedUsers", deactiveAssignedUsers);
            }
        } catch (Exception e) {
            usersData.put("error", "Failed to load users data: " + e.getMessage());
        }

        return usersData;
    }

    private Map<String, Object> getNotesData(Long companyId, Long userId, User.Role role) {
        Map<String, Object> notesData = new HashMap<>();
        
        try {
            // This would need to be implemented based on your notes structure
            // For now, returning empty data
            notesData.put("totalNotes", 0L);
            notesData.put("publicNotes", 0L);
            notesData.put("userNotes", 0L);
        } catch (Exception e) {
            notesData.put("error", "Failed to load notes data: " + e.getMessage());
        }

        return notesData;
    }

    private Map<String, Object> getTodayFollowUpsData(Long companyId, Long userId, User.Role role) {
        Map<String, Object> followUpsData = new HashMap<>();
        
        try {
            // This would need to be implemented based on your follow-ups structure
            // For now, returning empty data
            followUpsData.put("totalFollowUps", 0L);
            followUpsData.put("userFollowUps", 0L);
        } catch (Exception e) {
            followUpsData.put("error", "Failed to load follow-ups data: " + e.getMessage());
        }

        return followUpsData;
    }

    private Map<String, Object> getTodayEventsData(Long companyId, Long userId, User.Role role) {
        Map<String, Object> eventsData = new HashMap<>();
        
        try {
            // Call the notes controller endpoint to get today's events
            // This is a simplified approach - in production you might want to inject the service directly
            eventsData.put("totalEvents", 0L);
            eventsData.put("userEvents", 0L);
            eventsData.put("message", "Events data available via /api/companies/{companyId}/notes/dashboard/today-events");
        } catch (Exception e) {
            eventsData.put("error", "Failed to load events data: " + e.getMessage());
        }

        return eventsData;
    }
}
