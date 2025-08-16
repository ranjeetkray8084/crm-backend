package com.example.real_estate_crm.dto;

import com.example.real_estate_crm.model.User.Role;
import lombok.Data;

@Data
public class CreateUserRequest {
    private String name;
    private String email;
    private String phone;
    private String password;
    private String confirmPassword;
    private Role role;
    private Long companyId;
    private Long adminId; // Optional, only for USER role
}
