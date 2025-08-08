package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Lead.LeadStatus;
import com.example.real_estate_crm.model.Property;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.LeadRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.LeadDao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class LeadDaoImpl implements LeadDao {

    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private void setTenantContext(Long companyId) {
        // Implement context switch logic if needed for multitenancy (e.g., schema switching)
    }

    private Company getCompanyById(Long companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new EntityNotFoundException("Company not found with ID: " + companyId));
    }

    @Override
    public List<Lead> getAllLeads(Long companyId) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        return leadRepository.findByCompany(company);
    }

    @Override
    public Lead getById(Long companyId, Long id) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        return leadRepository.findByLeadIdAndCompany(id, company)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with ID: " + id + " for company: " + companyId));
    }

    @Override
    public Lead addLead(Long companyId, Lead lead) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        lead.setCompany(company);
        return leadRepository.save(lead);
    }

    @Override
    public Lead updateLead(Long companyId, Lead lead) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        Lead existing = leadRepository.findByLeadIdAndCompany(lead.getLeadId(), company)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with ID: " + lead.getLeadId() + " for company: " + companyId));
        
        // Update fields
        existing.setName(lead.getName());
        existing.setEmail(lead.getEmail());
        existing.setPhone(lead.getPhone());
        existing.setSource(lead.getSource());
        existing.setReferenceName(lead.getReferenceName());
        existing.setStatus(lead.getStatus());
        existing.setAction(lead.getAction());
        existing.setAssignedTo(lead.getAssignedTo());
        existing.setBudget(lead.getBudget());
        existing.setRequirement(lead.getRequirement());
        existing.setCreatedBy(lead.getCreatedBy());

        return leadRepository.save(existing);
    }

    @Override
    public void deleteById(Long companyId, Long id) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        Lead lead = leadRepository.findByLeadIdAndCompany(id, company)
                .orElseThrow(() -> new EntityNotFoundException("Lead not found with ID: " + id + " for company: " + companyId));
        leadRepository.delete(lead);
    }

    @Override
    public Optional<Lead> assignLead(Long companyId, Long leadId, Long userId) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        Optional<Lead> optionalLead = leadRepository.findByLeadIdAndCompany(leadId, company);
        if (optionalLead.isPresent()) {
            Lead lead = optionalLead.get();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));
            lead.assignTo(user);
            return Optional.of(leadRepository.save(lead));
        }
        return Optional.empty();
    }

    @Override
    public Optional<Lead> unassignLead(Long companyId, Long leadId) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        Optional<Lead> optionalLead = leadRepository.findByLeadIdAndCompany(leadId, company);
        if (optionalLead.isPresent()) {
            Lead lead = optionalLead.get();
            lead.unassign();
            return Optional.of(leadRepository.save(lead));
        }
        return Optional.empty();
    }

    @Override
    public List<Lead> getLeadsByStatus(Long companyId, LeadStatus status) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        return leadRepository.findByStatusAndCompany(status, company);
    }

    @Override
    public List<Lead> getLeadsBySource(Long companyId, String source) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        return leadRepository.findBySourceIgnoreCaseAndCompany(source, company);
    }

    @Override
    public List<Lead> getLeadsByCreatedBy(Long companyId, String createdBy) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        Long userId = Long.parseLong(createdBy);
        return leadRepository.findByCreatorIdAndCompany(userId, company);
    }

    @Override
    public List<Lead> getLeadsByAssignedUserId(Long companyId, Long userId) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        return leadRepository.findByAssignedToUserIdAndCompany(userId, company);
    }

    @Override
    public List<Lead> searchLeadsByName(Long companyId, String name) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        return leadRepository.findByNameContainingIgnoreCaseAndCompany(name, company);
    }

    @Override
    public Optional<Lead> updateLeadStatus(Long companyId, Long leadId, LeadStatus status) {
        setTenantContext(companyId);
        Company company = getCompanyById(companyId);
        Optional<Lead> optionalLead = leadRepository.findByLeadIdAndCompany(leadId, company);
        if (optionalLead.isPresent()) {
            Lead lead = optionalLead.get();
            lead.setStatus(status);
            return Optional.of(leadRepository.save(lead));
        }
        return Optional.empty();
    }

    @Override
    public boolean updateStatus(Long companyId, Long leadId, String statusStr) {
        setTenantContext(companyId);
        Lead lead = entityManager.createQuery(
                "SELECT l FROM Lead l WHERE l.company.id = :companyId AND l.leadId = :leadId", Lead.class)
                .setParameter("companyId", companyId)
                .setParameter("leadId", leadId)
                .getSingleResult();

        if (lead == null) return false;

        try {
            LeadStatus statusEnum = LeadStatus.valueOf(statusStr.toUpperCase());
            lead.setStatus(statusEnum);
            entityManager.merge(lead);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public List<Lead> getLeadsPaginated(Long companyId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("leadId").descending());
        Page<Lead> leadsPage = leadRepository.findByCompany_Id(companyId, pageable);
        return leadsPage.getContent();
    }
    
    @Override
    public Page<Lead> getLeadsByAssignedUserId(Long companyId, Long userId, Pageable pageable) {
        return leadRepository.findByCompanyIdAndAssignedTo_UserId(companyId, userId, pageable);
    }
    
    @Override
    public Page<Lead> getLeadsByCreatedBy(Long companyId, String userId, Pageable pageable) {
        return leadRepository.findByCompanyIdAndCreatedBy_UserId(companyId, Long.parseLong(userId), pageable);
    }

    @Override
    public Page<Lead> searchLeads(Long companyId, String search, String status,
                                  BigDecimal minBudget, BigDecimal maxBudget, Long createdBy,
                                  String source, String action, Pageable pageable) {
        return leadRepository.searchLeads(companyId, search, status, minBudget, maxBudget, createdBy, source, action, pageable);
    }




    public Page<Lead> searchLeadsCreatedOrAssigned(Long companyId, Long userId, String search, String status,
            BigDecimal minBudget, BigDecimal maxBudget,
            String source, String action, Pageable pageable) {
return leadRepository.searchLeadsCreatedOrAssigned(companyId, userId, search, status, minBudget, maxBudget, source, action, pageable);
}



}
