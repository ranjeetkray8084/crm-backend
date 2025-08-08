package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.PropertyRemark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyRemarkRepository extends JpaRepository<PropertyRemark, Long> {

    List<PropertyRemark> findByProperty_PropertyId(Long propertyId);

}
