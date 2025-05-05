package com.example.real_estate_crm.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Lead.Action;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.LeadRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.LeadDao;

import jakarta.transaction.Transactional;

@Service
public class LeadDaoImpl implements LeadDao {

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<Lead> getAllLeads() {
        return leadRepository.findAll();
    }

    @Override
    public Lead getById(Long id) {
        return leadRepository.findById(id).orElse(null);
    }

    @Override
    public Lead addLead(Lead lead) {
        return leadRepository.save(lead);
    }

    @Override
    public Lead updateLead(Lead lead) {
        Lead existing = leadRepository.findById(lead.getLeadId())
                .orElseThrow(() -> new RuntimeException("Lead not found with id: " + lead.getLeadId()));

        existing.setName(lead.getName());
        existing.setEmail(lead.getEmail());
        existing.setPhone(lead.getPhone());
        existing.setSource(lead.getSource());
        existing.setStatus(lead.getStatus());

        // ✅ New fields added
        existing.setBudget(lead.getBudget());
        existing.setRequirement(lead.getRequirement());
        existing.setRemark(lead.getRemark());

//        if (lead.getAssignedTo() != null) {
//            existing.setAssignedTo(lead.getAssignedTo());
//        } else {
//            existing.setAssignedTo(null);
//        }

        // Optional: set updatedAt if you have such a field
        existing.setUpdatedAt(LocalDateTime.now());

        return leadRepository.save(existing);
    }


    @Override
    public void deleteById(Long id) {
        leadRepository.deleteById(id);
    }

    @Override
    @Transactional
    public Optional<Lead> assignLead(Long leadId, Long userId) {
        Optional<Lead> leadOptional = leadRepository.findById(leadId);
        if (leadOptional.isEmpty()) return Optional.empty();

        Lead lead = leadOptional.get();
        
        if (!Lead.Action.NEW.equals(lead.getStatus()) && !Lead.Action.UNASSIGNED.equals(lead.getAction())) {
            throw new IllegalStateException("Lead must be NEW or UNASSIGNED to be assigned");
        }

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) return Optional.empty();

        lead.assignTo(userOptional.get());  // uses your business logic method
        return Optional.of(leadRepository.save(lead));
    }


    @Override
    public Optional<Lead> unassignLead(Long leadId) {
        return leadRepository.findById(leadId)
                .map(lead -> {
                    lead.setAssignedTo(null);
                    lead.setAction(Action.UNASSIGNED);
                    lead.setUpdatedAt(LocalDateTime.now());
                    return leadRepository.save(lead);
                });
    }

  
    @Override
    public List<Lead> getLeadsBySource(String source) {
        return leadRepository.findAll().stream()
                .filter(lead -> source.equalsIgnoreCase(lead.getSource()))
                .toList();
    }

 
    @Override
    public List<Lead> getLeadsByAssignedUserId(Long userId) {
        return leadRepository.findByAssignedTo(
                userRepository.findById(userId).orElse(null)
        );
    }

    @Override
    public List<Lead> searchLeadsByName(String name) {
        return leadRepository.findAll().stream()
                .filter(lead -> lead.getName() != null && lead.getName().toLowerCase().contains(name.toLowerCase()))
                .toList();
    }
    
    @Override
    @Transactional
    public Optional<Lead> updateLeadStatus(Long leadId, Lead.LeadStatus status) {
        Optional<Lead> optionalLead = leadRepository.findById(leadId);
        optionalLead.ifPresent(lead -> {
            lead.setStatus(status); // ✅ set new status
        });
        return optionalLead;
    }


    @Override
    public List<Lead> getLeadsByStatus(String status) {
        // Assuming you have a list of leads stored somewhere (e.g., in a database or a collection)
        List<Lead> allLeads = getAllLeads(); // Example method to get all leads

        // Use Java Streams to filter leads by the provided status
        return allLeads.stream()
                       .filter(lead -> lead.getStatus().equals(status))
                       .collect(Collectors.toList());
    }

	@Override
	public List<Lead> getLeadsByCreatedBy(String createdBy) {
		// TODO Auto-generated method stub
		return leadRepository.findByCreatedBy(createdBy);
	}
    


}
