package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.service.dao.CompanyDao;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public class CompanyDaoImpl implements CompanyDao {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Save a new company to the database.
     */
    @Override
    public Company save(Company company) {
        entityManager.persist(company);
        return company;
    }

    /**
     * Find a company by its ID.
     */
    @Override
    public Company findById(Long id) {
        return entityManager.find(Company.class, id);
    }

    /**
     * Retrieve all companies.
     */
    @Override
    public List<Company> findAll() {
        return entityManager.createQuery("SELECT c FROM Company c", Company.class).getResultList();
    }

    /**
     * Update an existing company.
     */
    @Override
    public Company update(Company company) {
        return entityManager.merge(company);
    }

    /**
     * Delete a company by ID.
     */
    @Override
    public void delete(Long id) {
        Company company = findById(id);
        if (company != null) {
            entityManager.remove(company);
        }
    }

    /**
     * Search companies by name (partial match).
     */
    @Override
    public List<Company> searchByName(String name) {
        return entityManager.createQuery(
                "SELECT c FROM Company c WHERE LOWER(c.name) LIKE LOWER(:name)", Company.class)
                .setParameter("name", "%" + name + "%")
                .getResultList();
    }

    /**
     * Find all companies created by a specific developer.
     */
    @Override
    public List<Company> findByDeveloper(User developer) {
        return entityManager.createQuery(
                "SELECT c FROM Company c WHERE c.developer = :developer", Company.class)
                .setParameter("developer", developer)
                .getResultList();
    }

    /**
     * Get total count of all companies.
     */
    @Override
    public long getTotalCount() {
        return entityManager.createQuery("SELECT COUNT(c) FROM Company c", Long.class)
                .getSingleResult();
    }
}
