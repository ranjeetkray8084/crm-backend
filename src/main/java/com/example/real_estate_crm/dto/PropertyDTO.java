package com.example.real_estate_crm.dto;

import com.example.real_estate_crm.model.Property;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDTO {
    private Long propertyId;
    private String propertyName;
    private String type;
    private String bhk;
    private String size;

    private String ownerName;
    private String ownerContact;
    private BigDecimal price;

    private String sector;
    private String source;

    private String unitDetails;
    private String location;
    private String floor;
    private String referenceName;

    private String createdByName;
    private Long createdById;

    private Property.Status status;
    private LocalDateTime createdAt;

    public static PropertyDTO from(Property property, boolean hideSensitive) {
        PropertyDTO dto = new PropertyDTO();
        dto.setPropertyId(property.getPropertyId());
        dto.setPropertyName(property.getPropertyName());
        dto.setType(property.getType());
        dto.setBhk(property.getBhk());
        dto.setSize(property.getSize());
        dto.setOwnerName(property.getOwnerName());
        dto.setOwnerContact(hideSensitive ? "🔒 Hidden" : property.getOwnerContact());
        dto.setPrice(property.getPrice());
        dto.setSector(property.getSector());
        dto.setSource(property.getSource());
        dto.setUnitDetails(hideSensitive ? "🔒 Hidden" : property.getUnitDetails());
        dto.setLocation(property.getLocation());
        dto.setFloor(property.getFloor());
        dto.setReferenceName(property.getReferenceName());
        dto.setStatus(property.getStatus());
        dto.setCreatedAt(property.getCreatedAt());

        if (property.getCreatedBy() != null) {
            dto.setCreatedByName(property.getCreatedBy().getName());
            dto.setCreatedById(property.getCreatedBy().getUserId());
        } else {
            dto.setCreatedByName("Unknown");
            dto.setCreatedById(null);
        }

        return dto;
    }

	
}
