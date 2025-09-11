package com.example.real_estate_crm.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ContactRequest {
    private String name;
    
    @JsonProperty("Phone Number")
    private String number;
    
    private String email;
    private String message;
}

