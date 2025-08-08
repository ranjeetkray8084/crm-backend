package com.example.real_estate_crm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.real_estate_crm.model.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    Optional<Company> findByEmail(String email); // Example custom query

    boolean existsByEmail(String email);
    
    @Query("SELECT c FROM Company c WHERE c.name LIKE %:name%")
    List<Company> searchByName(@Param("name") String name);
}
