package com.example.real_estate_crm.dto;

import lombok.Data;

@Data
public class CompanyDTO {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private Integer maxUsers;
    private Integer maxAdmins;
}
