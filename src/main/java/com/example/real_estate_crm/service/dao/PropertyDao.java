package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.Property.Status;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyDao {

    List<Property> getAllProperties();

    Property findById(Long id);

    Property addProperty(Property property);

    Property updateProperty(Property property);

    void deleteById(Long id);

    // 🔍 Filter by property status: For Sale / Rent / Rent Out / Sold Out
    List<Property> getPropertiesByStatus(Status status);

    // 🔍 Filter by sector/area
    List<Property> getPropertiesBySector(String sector);

    // 🔍 Filter by lead source: social media / cold call / project call / reference
    List<Property> getPropertiesBySource(String source);

    // 🔍 Filter by price range - Adjusted to handle price as a string
    List<Property> getPropertiesByPriceRange(String minPrice, String maxPrice);

    // 🔍 Search by property name
    List<Property> searchPropertiesByName(String name);

    // 🔍 New: Filter by type (Commercial / Residential)
    List<Property> getPropertiesByType(String type);

    // 🔍 New: Filter by BHK (e.g., 1BHK, 2BHK)
    List<Property> getPropertiesByBhk(String bhk);

    // 🔍 New: Filter by owner contact
    List<Property> getPropertiesByOwnerContact(String contact);
    List<Property> getPropertiesByCreatedBy(Long createdBy);

}
