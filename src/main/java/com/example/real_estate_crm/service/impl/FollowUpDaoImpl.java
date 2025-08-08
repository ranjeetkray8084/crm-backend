package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.FollowUp;
import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.FollowUpRepository;
import com.example.real_estate_crm.repository.LeadRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.FollowUpDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class FollowUpDaoImpl implements FollowUpDao {

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private CompanyRepository companyRepository;
    
    @Autowired
    private LeadRepository leadRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<FollowUp> getAllFollowUps(Long companyId) {
        return followUpRepository.findByCompany_Id(companyId);
    }

    @Override
    public FollowUp findById(Long companyId, Long id) {
        return followUpRepository.findByFollowupIdAndCompany_Id(id, companyId).orElse(null);
    }

    @Override
    public FollowUp addFollowUp(Long companyId, FollowUp followUp) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid company ID: " + companyId));
        
        // ✅ Fetch Lead from DB
        Long leadId = followUp.getLead().getLeadId();
        Lead lead = leadRepository.findById(leadId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid lead ID: " + leadId));

        // ✅ Fetch User from DB (if you're passing user ID too)
        Long userId = followUp.getUser().getUserId();
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));

        followUp.setCompany(company);
        followUp.setLead(lead);
        followUp.setUser(user);

        return followUpRepository.save(followUp);
    }



    @Override
    public FollowUp updateFollowUp(Long companyId, FollowUp followUp) {
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            throw new IllegalArgumentException("Invalid company ID: " + companyId);
        }
        followUp.setCompany(company);
        return followUpRepository.save(followUp);
    }

    @Override
    public void deleteById(Long companyId, Long id) {
        FollowUp followUp = findById(companyId, id);
        if (followUp != null) {
            followUpRepository.delete(followUp);
        }
    }

    @Override
    public List<FollowUp> getTodayFollowUps(Long companyId, LocalDateTime start, LocalDateTime end) {
        return followUpRepository.findTodayFollowUps(companyId, start, end);
    }
}
