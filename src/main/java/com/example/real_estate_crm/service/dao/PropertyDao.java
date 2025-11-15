package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.dto.PropertySearchRequest;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.Property.Status;

import java.util.List;
import java.util.Optional;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface PropertyDao {

    // 🏢 Get all properties for a specific company
    List<Property> getAllProperties(Long companyId);

    // 🔍 Find a property by ID and companyId to ensure ownership
    Property findById(Long propertyId, Long companyId);

    // ➕ Add a property (company must be set in the Property entity)
    Property addProperty(Property property);

    // ✏️ Update a property (ensure company is respected)
    Property updateProperty(Property property);

    // ❌ Delete property by ID and companyId to ensure ownership
    void deleteById(Long propertyId, Long companyId);

    // 🔍 Filter by property status: For Sale / Rent / Rent Out / Sold Out
    List<Property> getPropertiesByStatus(Long companyId, Status status);

    // 🔍 Filter by sector/area
    List<Property> getPropertiesBySector(Long companyId, String sector);

    // 🔍 Filter by lead source: social media / cold call / project call / reference
    List<Property> getPropertiesBySource(Long companyId, String source);

    // 🔍 Filter by price range (using Double instead of String for correctness)

    // 🔍 Search by property name (partial match)
    List<Property> searchPropertiesByName(Long companyId, String name);

    // 🔍 Filter by type (Commercial / Residential)
    List<Property> getPropertiesByType(Long companyId, String type);

    // 🔍 Filter by BHK (e.g., 1BHK, 2BHK)
    List<Property> getPropertiesByBhk(Long companyId, String bhk);

    // 🔍 Filter by owner contact (partial match)
    List<Property> getPropertiesByOwnerContact(Long companyId, String contact);

    // 🔍 Filter by createdBy (creator user ID)
    List<Property> getPropertiesByCreatedBy(Long companyId, Long createdBy);
    
    // 🔍 Find property by external property ID
    Property findByExternalPropertyId(String externalPropertyId);
    
    // 🔍 Find property by external property ID and company
    Property findByExternalPropertyIdAndCompany(String externalPropertyId, Long companyId);
    
    List<Property> advancedSearch(PropertySearchRequest request);
    
    Page<Property> advancedSearchPaged(PropertySearchRequest request, Pageable pageable);

    
}
