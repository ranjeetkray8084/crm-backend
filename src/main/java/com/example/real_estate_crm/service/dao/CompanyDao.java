package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;

import java.util.List;

public interface CompanyDao {

    Company save(Company company);

    Company findById(Long id);

    List<Company> findAll();

    Company update(Company company);

    void delete(Long id);

    List<Company> searchByName(String name);

	List<Company> findByDeveloper(User developer);

	long getTotalCount();

}
