package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.Lead.LeadStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
/**
 * Lead Data Access Object (DAO) interface for managing Leads in a multi-tenant system.
 * All methods require a companyId parameter to ensure tenant-specific data access.
 */
public interface LeadDao {

    List<Lead> getAllLeads(Long companyId);

    Lead getById(Long companyId, Long id);

    Lead addLead(Long companyId, Lead lead);

    Lead updateLead(Long companyId, Lead lead);

    void deleteById(Long companyId, Long id);

    Optional<Lead> assignLead(Long companyId, Long leadId, Long userId);

    Optional<Lead> unassignLead(Long companyId, Long leadId);

    List<Lead> getLeadsByStatus(Long companyId, LeadStatus status);

    List<Lead> getLeadsBySource(Long companyId, String source);

    List<Lead> getLeadsByCreatedBy(Long companyId, String createdBy);

    List<Lead> getLeadsByAssignedUserId(Long companyId, Long userId);

    List<Lead> searchLeadsByName(Long companyId, String name);
    
    boolean updateStatus(Long companyId, Long leadId, String status);

    Optional<Lead> updateLeadStatus(Long companyId, Long leadId, LeadStatus status);

    List<Lead> getLeadsPaginated(Long companyId, int page, int size);
    Page<Lead> getLeadsByAssignedUserId(Long companyId, Long userId, Pageable pageable);
    
    Page<Lead> getLeadsByCreatedBy(Long companyId, String userId, Pageable pageable);

    Page<Lead> searchLeads(Long companyId, String search, String status,
            BigDecimal minBudget, BigDecimal maxBudget, Long createdBy,
            String source, String action, Pageable pageable);
    
    Page<Lead> searchLeadsCreatedOrAssigned(Long companyId, Long userId, String search, String status,
            BigDecimal minBudget, BigDecimal maxBudget,
            String source, String action, Pageable pageable);


}
