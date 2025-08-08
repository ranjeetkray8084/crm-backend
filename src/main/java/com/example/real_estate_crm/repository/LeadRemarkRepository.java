package com.example.real_estate_crm.repository;


import com.example.real_estate_crm.model.LeadRemark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadRemarkRepository extends JpaRepository<LeadRemark, Long> {

	List<LeadRemark> findByLead_LeadId(Long leadId);

}
