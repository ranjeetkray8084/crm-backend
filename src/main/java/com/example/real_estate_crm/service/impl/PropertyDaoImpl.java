package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.dto.PropertySearchRequest;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.Property.Status;
import com.example.real_estate_crm.repository.PropertyRepository;
import com.example.real_estate_crm.service.dao.PropertyDao;
import com.example.real_estate_crm.specification.PropertySpecification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@Repository
public class PropertyDaoImpl implements PropertyDao {

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public List<Property> getAllProperties(Long companyId) {
        return propertyRepository.findByCompany_Id(companyId);
    }

    @Override
    public Property findById(Long id, Long companyId) {
        return propertyRepository.findById(id)
                .filter(p -> p.getCompany() != null && p.getCompany().getId().equals(companyId))
                .orElse(null);
    }

    @Override
    public Property addProperty(Property property) {
        return propertyRepository.save(property);
    }

    @Override
    public Property updateProperty(Property incoming) {
        Property existing = propertyRepository.findById(incoming.getPropertyId())
                .orElseThrow(() -> new RuntimeException("Property not found"));

        // Enforce company ID check
        if (existing.getCompany() == null || !existing.getCompany().getId().equals(incoming.getCompany().getId())) {
            throw new RuntimeException("Company ID mismatch! Unauthorized update attempt.");
        }

        existing.setPropertyName(incoming.getPropertyName());
        existing.setType(incoming.getType());
        existing.setBhk(incoming.getBhk());
        existing.setSize(incoming.getSize());
        existing.setOwnerName(incoming.getOwnerName());
        existing.setOwnerContact(incoming.getOwnerContact());
        existing.setPrice(incoming.getPrice());
        existing.setSector(incoming.getSector());
        existing.setStatus(incoming.getStatus());
        existing.setRemarks(incoming.getRemarks());

        return propertyRepository.save(existing);
    }

    @Override
    public void deleteById(Long id, Long companyId) {
        propertyRepository.findById(id)
                .filter(p -> p.getCompany() != null && p.getCompany().getId().equals(companyId))
                .ifPresentOrElse(
                        propertyRepository::delete,
                        () -> {
                            throw new RuntimeException("Property not found or unauthorized access!");
                        }
                );
    }

    @Override
    public List<Property> getPropertiesByStatus(Long companyId, Status status) {
        return propertyRepository.findByCompany_IdAndStatus(companyId, status);
    }

    @Override
    public List<Property> getPropertiesBySector(Long companyId, String sector) {
        return propertyRepository.findByCompany_IdAndBhkIgnoreCase(companyId, sector);
    }

    @Override
    public List<Property> getPropertiesBySource(Long companyId, String source) {
        return propertyRepository.findByCompany_IdAndBhkIgnoreCase(companyId, source);
    }

    private BigDecimal parsePrice(String price) {
        if (price != null) {
            price = price.toLowerCase().trim();
            try {
                if (price.endsWith("lk")) {
                    return new BigDecimal(price.replace("lk", "").trim()).multiply(new BigDecimal(100000));
                } else if (price.endsWith("k")) {
                    return new BigDecimal(price.replace("k", "").trim()).multiply(new BigDecimal(1000));
                } else {
                    return new BigDecimal(price);
                }
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

   

    @Override
    public List<Property> searchPropertiesByName(Long companyId, String name) {
        return propertyRepository.findByCompany_Id(companyId).stream()
                .filter(property -> property.getPropertyName() != null &&
                        property.getPropertyName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Property> getPropertiesByType(Long companyId, String type) {
        return propertyRepository.findByCompany_IdAndBhkIgnoreCase(companyId, type);
    }

    @Override
    public List<Property> getPropertiesByBhk(Long companyId, String bhk) {
        return propertyRepository.findByCompany_IdAndBhkIgnoreCase(companyId, bhk);
    }

    @Override
    public List<Property> getPropertiesByOwnerContact(Long companyId, String contact) {
        return propertyRepository.findByCompany_IdAndOwnerContactContaining(companyId, contact);
    }

    @Override
    public List<Property> getPropertiesByCreatedBy(Long companyId, Long createdByUserId) {
        return propertyRepository.findByCompany_IdAndCreatedBy_UserId(companyId, createdByUserId);
    }


    

    @Override
    public List<Property> advancedSearch(PropertySearchRequest request) {
        Specification<Property> spec = PropertySpecification.getProperties(request);
        return propertyRepository.findAll(spec);
    }

  

    @Override
    public Page<Property> advancedSearchPaged(PropertySearchRequest request, Pageable pageable) {
        Specification<Property> spec = PropertySpecification.getProperties(request);
        return propertyRepository.findAll(spec, pageable);
    }



 

   

	
}
