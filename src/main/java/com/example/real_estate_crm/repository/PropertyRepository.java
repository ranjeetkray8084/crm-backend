package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Property;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property> {

    // Find properties by company and status
    List<Property> findByCompany_IdAndStatus(Long companyId, Property.Status status);

    // Find properties by company and type (Commercial / Residential)
    List<Property> findByCompany_IdAndTypeIgnoreCase(Long companyId, String type);

    // Find properties by company and BHK type
    List<Property> findByCompany_IdAndBhkIgnoreCase(Long companyId, String bhk);

    // Find properties by company and sector
    List<Property> findByCompany_IdAndSectorIgnoreCase(Long companyId, String sector);

    // Find by company and owner contact (partial match)
    List<Property> findByCompany_IdAndOwnerContactContaining(Long companyId, String ownerContact);

    // Find by company and source
    List<Property> findByCompany_IdAndSourceIgnoreCase(Long companyId, String source);

    // Search properties by name containing
    List<Property> findByCompany_IdAndPropertyNameContainingIgnoreCase(Long companyId, String propertyName);

    // Find by company, creator, and status
    List<Property> findByCompany_IdAndCreatedBy_UserIdAndStatus(Long companyId, Long createdByUserId, Property.Status status);

    // ✅ Find by company, creator, and type
    List<Property> findByCompany_IdAndCreatedBy_UserIdAndTypeIgnoreCase(Long companyId, Long createdByUserId, String type);

    // ✅ Find by company and creator
    List<Property> findByCompany_IdAndCreatedBy_UserId(Long companyId, Long createdByUserId);

    // Find by company
    List<Property> findByCompany_Id(Long companyId);

    // Paginated fetch
    Page<Property> findByCompanyId(Long companyId, Pageable pageable);

    // Find properties with reminder dates between two dates
    List<Property> findByStatusAndReminderDateBetween(Property.Status status, LocalDateTime startDate, LocalDateTime endDate);
    
    // Find properties with reminder dates between two dates (for scheduled notifications)
    List<Property> findByReminderDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // ✅ Count all properties (optional)
    @Query("SELECT COUNT(p) FROM Property p")
    long countAllProperties();

    // ✅ Count by company
    long countByCompany_Id(Long companyId);

    // ✅ Count by company and creator
    long countByCompanyIdAndCreatedByUserId(Long companyId, Long userId);

    // ✅ Count by company and public properties

    // ✅ Count by company and status
    long countByCompany_IdAndStatus(Long companyId, Property.Status status);

    // Find properties by company excluding DROPPED status (paginated)
    Page<Property> findByCompany_IdAndStatusNot(Long companyId, Property.Status status, Pageable pageable);

    // Find properties by company excluding DROPPED status (non-paginated)
    List<Property> findByCompany_IdAndStatusNot(Long companyId, Property.Status status);

    // Count properties by company excluding DROPPED status
    long countByCompany_IdAndStatusNot(Long companyId, Property.Status status);

    // Find property by external property ID
    Property findByExternalPropertyId(String externalPropertyId);

    // Find property by external property ID and company
    Property findByExternalPropertyIdAndCompany_Id(String externalPropertyId, Long companyId);
}
