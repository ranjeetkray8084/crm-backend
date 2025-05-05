package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Lead.LeadStatus;

import java.util.List;
import java.util.Optional;

public interface LeadDao {
    List<Lead> getAllLeads();

    Lead getById(Long id);

    Lead addLead(Lead lead);

    Lead updateLead(Lead lead);

    void deleteById(Long id);

    Optional<Lead> assignLead(Long leadId, Long userId);

    Optional<Lead> unassignLead(Long leadId);

    // 🔍 Additional methods for filtering
    List<Lead> getLeadsByStatus(String status); // updated to use enum
    
    List<Lead> getLeadsBySource(String source);
    
    List<Lead> getLeadsByCreatedBy(String createdBy);


    List<Lead> getLeadsByAssignedUserId(Long userId);

    List<Lead> searchLeadsByName(String name);

    // ✅ New method for updating status
    Optional<Lead> updateLeadStatus(Long leadId, LeadStatus status);
}
