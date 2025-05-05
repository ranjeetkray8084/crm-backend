package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.Property.Status;
import com.example.real_estate_crm.repository.PropertyRepository;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.PropertyDao;
import com.example.real_estate_crm.service.dao.UserDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    @Autowired
    private PropertyDao propertyService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDao userService;
    
    @Autowired
    PropertyRepository propertyRepository;


    @GetMapping
    public List<Property> getAllProperties() {
        return propertyService.getAllProperties();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Property> getProperty(@PathVariable Long id) {
        Property property = propertyService.findById(id);
        if (property != null) {
            return ResponseEntity.ok(property);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Property> createProperty(@RequestBody Property property) {
        Property created = propertyService.addProperty(property);

        // Notify all Admins if the property was created by a User
        if (property.getCreatedBy() != null) {
            userService.findById(property.getCreatedBy()).ifPresent(creator -> {
                if ("User".equalsIgnoreCase(creator.getRole().name())) {
                    String message = "A new property \"" + created.getPropertyName() +
                                     "\" was created by " + creator.getName();

                    userService.getAllUsers().stream()
                            .filter(admin -> "Admin".equalsIgnoreCase(admin.getRole().name()))
                            .forEach(admin -> notificationService.sendNotification(admin.getUserId(), message));
                }
            });
        }

        return ResponseEntity.ok(created);
    }


    @PutMapping
    public ResponseEntity<Property> updateProperty(@RequestBody Property property) {
        Property updated = propertyService.updateProperty(property);

        // Send a notification (example: notify userId 1 for demo purpose)
        notificationService.sendNotification(1L, "Property updated: " + updated.getPropertyName());

        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProperty(@PathVariable Long id) {
        propertyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    
    @GetMapping("/created-by/{userId}")
    public ResponseEntity<List<Property>> getPropertiesByCreatedBy(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(propertyService.getPropertiesByCreatedBy(userId));
    }



    @GetMapping("/status/{status}")
    public List<Property> getPropertiesByStatus(@PathVariable Status status) {
        return propertyService.getPropertiesByStatus(status);
    }

    @GetMapping("/sector/{sector}")
    public List<Property> getPropertiesBySector(@PathVariable String sector) {
        return propertyService.getPropertiesBySector(sector);
    }

    @GetMapping("/source/{source}")
    public List<Property> getPropertiesBySource(@PathVariable String source) {
        return propertyService.getPropertiesBySource(source);
    }

    @GetMapping("/price-range")
    public List<Property> getPropertiesByPriceRange(@RequestParam String minPrice,
                                                    @RequestParam String maxPrice) {
        return propertyService.getPropertiesByPriceRange(minPrice, maxPrice);
    }

    @GetMapping("/search")
    public List<Property> searchPropertiesByName(@RequestParam String name) {
        return propertyService.searchPropertiesByName(name);
    }

    @GetMapping("/type/{type}")
    public List<Property> getPropertiesByType(@PathVariable String type) {
        return propertyService.getPropertiesByType(type);
    }

    @GetMapping("/bhk/{bhk}")
    public List<Property> getPropertiesByBhk(@PathVariable String bhk) {
        return propertyService.getPropertiesByBhk(bhk);
    }

    @GetMapping("/owner-contact/{contact}")
    public List<Property> getPropertiesByOwnerContact(@PathVariable String contact) {
        return propertyService.getPropertiesByOwnerContact(contact);
    }

    @PutMapping("/{propertyId}")
    public ResponseEntity<Property> updatePropertyById(@PathVariable Long propertyId, @RequestBody Property updatedProperty) {
        Property existingProperty = propertyService.findById(propertyId);
        if (existingProperty == null) {
            return ResponseEntity.notFound().build();
        }

        if (updatedProperty.getPropertyName() != null) {
            existingProperty.setPropertyName(updatedProperty.getPropertyName());
        }
        if (updatedProperty.getType() != null) {
            existingProperty.setType(updatedProperty.getType());
        }
        if (updatedProperty.getBhk() != null) {
            existingProperty.setBhk(updatedProperty.getBhk());
        }
        if (updatedProperty.getSize() != null) {
            existingProperty.setSize(updatedProperty.getSize());
        }
        if (updatedProperty.getOwnerName() != null) {
            existingProperty.setOwnerName(updatedProperty.getOwnerName());
        }
        if (updatedProperty.getOwnerContact() != null) {
            existingProperty.setOwnerContact(updatedProperty.getOwnerContact());
        }
        if (updatedProperty.getPrice() != null) {
            existingProperty.setPrice(updatedProperty.getPrice());
        }
        if (updatedProperty.getStatus() != null) {
            existingProperty.setStatus(updatedProperty.getStatus());
        }
        if (updatedProperty.getSector() != null) {
            existingProperty.setSector(updatedProperty.getSector());
        }
        if (updatedProperty.getSource() != null) {
            existingProperty.setSource(updatedProperty.getSource());
        }
        if (updatedProperty.getRemark() != null) {
            existingProperty.setRemark(updatedProperty.getRemark());
        }

        Property savedProperty = propertyService.updateProperty(existingProperty);

        // Notify on update

        return ResponseEntity.ok(savedProperty);
    }

}
