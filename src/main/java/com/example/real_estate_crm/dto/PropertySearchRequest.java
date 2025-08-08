package com.example.real_estate_crm.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
public class PropertySearchRequest {
    private Long companyId;
    private List<String> keywords;
    private String type;
    private String bhk;
    private String size;
    private String status;
    private String source;
    private Long createdBy;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
}
