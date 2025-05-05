package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    // Find properties by status
    List<Property> findByStatus(Property.Status status);

    // Find properties by type (Commercial / Residential)
    List<Property> findByType(String type);

    // Find properties by BHK type
    List<Property> findByBhk(String bhk);

    // Find properties by sector
    List<Property> findBySector(String sector);

    // Find by owner contact
    List<Property> findByOwnerContact(String ownerContact);

    // Find by source
    List<Property> findBySource(String source);

    // Search by price less than or equal
    List<Property> findByPriceLessThanEqual(String price);
 // Find by creator and status
    List<Property> findByCreatedByAndStatus(Long createdBy, Property.Status status);

    // Find by creator and type
    List<Property> findByCreatedByAndType(Long createdBy, String type);
    
    List<Property> findByCreatedBy(Long createdBy);


}
