package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.FollowUpRequest;
import com.example.real_estate_crm.model.FollowUp;
import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.FollowUpRepository;
import com.example.real_estate_crm.repository.LeadRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.FollowUpDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/{companyId}/followups")
public class FollowUpController {

    @Autowired
    private FollowUpDao followUpDao;
    
    @Autowired
    private FollowUpRepository followUpRepository; 
    
    @Autowired
    private LeadRepository leadRepository;
    
    @Autowired
    private UserRepository userRepository;

    // ✅ Get all follow-ups
    @GetMapping
    public ResponseEntity<List<FollowUp>> getAllFollowUps(@PathVariable Long companyId) {
        return ResponseEntity.ok(followUpDao.getAllFollowUps(companyId));
    }

    // ✅ Get follow-up by ID
    @GetMapping("/{id}")
    public ResponseEntity<FollowUp> getFollowUpById(@PathVariable Long companyId,
                                                    @PathVariable Long id) {
        FollowUp followUp = followUpDao.findById(companyId, id);
        return followUp != null ? ResponseEntity.ok(followUp) : ResponseEntity.notFound().build();
    }

    // ✅ Create new follow-up
    @PostMapping()
    public ResponseEntity<?> createFollowUp(@RequestBody FollowUpRequest request) {
        Lead lead = leadRepository.findById(request.getLeadId())
            .orElseThrow(() -> new RuntimeException("Lead not found"));
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));

        FollowUp followUp = new FollowUp();
        followUp.setNote(request.getNote());
        followUp.setFollowupDate(request.getFollowUpDate());
        followUp.setLead(lead);
        followUp.setUser(user);
        followUp.setCompany(lead.getCompany());

        FollowUp saved = followUpRepository.save(followUp);
        return ResponseEntity.ok(saved);
    }

    // ✅ Update follow-up
    @PutMapping
    public ResponseEntity<FollowUp> updateFollowUp(@PathVariable Long companyId,
                                                   @RequestBody FollowUp followUp) {
        FollowUp updated = followUpDao.updateFollowUp(companyId, followUp);
        return ResponseEntity.ok(updated);
    }

    // ✅ Delete follow-up
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFollowUp(@PathVariable Long companyId,
                                               @PathVariable Long id) {
        followUpDao.deleteById(companyId, id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Get today's follow-ups
    @GetMapping("/today")
    public ResponseEntity<List<FollowUp>> getTodayFollowUps(@PathVariable Long companyId) {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        return ResponseEntity.ok(followUpDao.getTodayFollowUps(companyId, startOfDay, endOfDay));
    }
}
