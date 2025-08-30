package com.example.real_estate_crm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PushTokenRequest {
    private String pushToken;
    private String deviceType; // "android", "ios", "web"
}
