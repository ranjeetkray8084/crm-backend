package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.Property.Status;
import com.example.real_estate_crm.repository.PropertyRepository;
import com.example.real_estate_crm.service.dao.PropertyDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class PropertyDaoImpl implements PropertyDao {

    @Autowired
    private PropertyRepository propertyRepository;

    @Override
    public List<Property> getAllProperties() {
        return propertyRepository.findAll();
    }

    @Override
    public Property findById(Long id) {
        return propertyRepository.findById(id).orElse(null);
    }

    @Override
    public Property addProperty(Property property) {
        return propertyRepository.save(property);
    }

    @Override
    public Property updateProperty(Property property) {
        return propertyRepository.save(property);
    }

    @Override
    public void deleteById(Long id) {
        propertyRepository.deleteById(id);
    }

    @Override
    public List<Property> getPropertiesByStatus(Status status) {
        return propertyRepository.findByStatus(status);
    }

    @Override
    public List<Property> getPropertiesBySector(String sector) {
        return propertyRepository.findBySector(sector);
    }

    @Override
    public List<Property> getPropertiesBySource(String source) {
        return propertyRepository.findBySource(source);
    }

    // Method to parse the string price (e.g., "5lk", "15k") into BigDecimal for comparison
    private BigDecimal parsePrice(String price) {
        if (price != null && price.contains("lk")) {
            String numericValue = price.replace("lk", "").trim();
            try {
                return new BigDecimal(numericValue).multiply(new BigDecimal(100000)); // Lakh (1 lakh = 100,000)
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO; // Return zero for invalid format
            }
        } else if (price != null && price.contains("k")) {
            String numericValue = price.replace("k", "").trim();
            try {
                return new BigDecimal(numericValue).multiply(new BigDecimal(1000)); // Thousand (1k = 1000)
            } catch (NumberFormatException e) {
                return BigDecimal.ZERO; // Return zero for invalid format
            }
        }
        return BigDecimal.ZERO; // Default if no recognized suffix
    }

    @Override
    public List<Property> getPropertiesByPriceRange(String minPrice, String maxPrice) {
        BigDecimal min = parsePrice(minPrice); // Parse the minPrice to BigDecimal
        BigDecimal max = parsePrice(maxPrice); // Parse the maxPrice to BigDecimal
        
        return propertyRepository.findAll().stream()
                .filter(property -> {
                    BigDecimal propertyPrice = parsePrice(property.getPrice());
                    return propertyPrice.compareTo(min) >= 0 && propertyPrice.compareTo(max) <= 0;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Property> searchPropertiesByName(String name) {
        return propertyRepository.findAll().stream()
                .filter(property -> property.getPropertyName() != null &&
                        property.getPropertyName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Property> getPropertiesByType(String type) {
        return propertyRepository.findByType(type);
    }

    @Override
    public List<Property> getPropertiesByBhk(String bhk) {
        return propertyRepository.findByBhk(bhk);
    }

    @Override
    public List<Property> getPropertiesByOwnerContact(String contact) {
        return propertyRepository.findByOwnerContact(contact);
    }
    
    @Override
	public List<Property> getPropertiesByCreatedBy(Long createdBy) {
		// TODO Auto-generated method stub
		return propertyRepository.findByCreatedBy(createdBy);
	}
}
