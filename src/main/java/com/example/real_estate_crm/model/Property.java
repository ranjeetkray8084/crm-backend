package com.example.real_estate_crm.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long propertyId;

    @NotBlank(message = "Property name is required")
    @Column(nullable = false)
    private String propertyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnoreProperties({"email", "password", "role", "phone", "createdAt", "updatedAt", "company", "notes", "leads"})
    private User createdBy;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    @JsonIgnore
    private Company company;

    private String type;
    private String bhk;
    private String size;

    private String ownerName;
    private String ownerContact;
    @Column(precision = 20, scale = 2, nullable=true)  // optional, adjust as per needs
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.AVAILABLE_FOR_SALE;

    private String sector;
    private String source;

    private String unitDetails;
    @Column(nullable = true)
    private String location;
//    
    @Column(nullable = true)
    private  String floor;


    @Column(name = "reference_name")
    private String referenceName;

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<PropertyRemark> remarks;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    // Summary getter for createdBy
    @JsonProperty("createdBy")
    public UserSummary getCreatedBySummary() {
        if (createdBy != null) {
            return new UserSummary(createdBy.getUserId(), createdBy.getName());
        }
        return null;
    }

    @JsonProperty("createdByName")
    public String getCreatedByName() {
        return createdBy != null ? createdBy.getName() : "Unknown";
    }

    public enum Status {
        AVAILABLE_FOR_SALE,
        AVAILABLE_FOR_RENT,
        RENT_OUT,
        SOLD_OUT
    }
}
