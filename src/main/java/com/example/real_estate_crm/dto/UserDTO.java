package com.example.real_estate_crm.dto;

import com.example.real_estate_crm.model.User;
import lombok.Data;

/**
 * Data Transfer Object for User entity.
 * Used to safely expose user data to the frontend without deep nested references.
 */
@Data
public class UserDTO {

    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String status;
    private String adminName;
    private Long companyId;
    private String companyName;

    public UserDTO(User user) {
        this.userId = user.getUserId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.phone = user.getPhone();
        this.role = user.getRole().name();
        this.status = user.getStatus() != null && user.getStatus() ? "active" : "inactive";

        // Admin info - only show admin for USER role
        if (user.getRole() == User.Role.USER) {
            this.adminName = (user.getAdmin() != null) ? user.getAdmin().getName() : "No Admin";
        } else {
            // ADMIN and DIRECTOR roles don't have admins
            this.adminName = "N/A";
        }

        // Company info
        if (user.getCompany() != null) {
            this.companyId = user.getCompany().getId();
            this.companyName = user.getCompany().getName();
        }
    }
}
