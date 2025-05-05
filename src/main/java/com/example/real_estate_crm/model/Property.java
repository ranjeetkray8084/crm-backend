package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "properties")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long propertyId;

    // P-Name
    private String propertyName;
    
 
    private Long createdBy;


    // Commercial / Residential
    private String type;

    // 1BHK / 2BHK / 3BHK etc.
    private String bhk;

    // e.g., 250/415 etc.
    private String size;

    // Owner name and contact number
    private String ownerName;
    private String ownerContact;

    private String price;

    // Available for Sale / Rent / Rent Out / Sold Out
    @Enumerated(EnumType.STRING)
    private Status status;

    // Sector / Area
    private String sector;

    // Source of lead: Social media / Cold call / Project call / Reference
    private String source;

    @Column(columnDefinition = "TEXT")
    private String remark;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Status {
        AVAILABLE_FOR_SALE,
        AVAILABLE_FOR_RENT,
        RENT_OUT,
        SOLD_OUT
    }
}
