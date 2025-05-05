package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    // Find leads by LeadStatus enum
    List<Lead> findByStatus(String status);

    // Find leads assigned to specific user
    List<Lead> findByAssignedTo(User assignedTo);

    // Find leads by source
    List<Lead> findBySourceIgnoreCase(String source);

    // Find leads by action (ASSIGNED, UNASSIGNED)
    List<Lead> findByAction(Lead.Action action);

    // Optional: search by partial name
    List<Lead> findByNameContainingIgnoreCase(String name);

    // Fixed: Find leads by createdBy field
    List<Lead> findByCreatedBy(String createdBy);
}
