package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.security.SecurityUtil;
import com.example.real_estate_crm.service.dao.CompanyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyDao companyDao;

    @Autowired
    public CompanyController(CompanyDao companyDao) {
        this.companyDao = companyDao;
    }

    // ✅ 1. Add New Company (Only Developer)
    @PostMapping("/add")
    public ResponseEntity<?> addCompany(@RequestBody Company company) {
        // Get current user (should be developer)
        User currentUser = SecurityUtil.getCurrentUser();
        System.out.println("🔍 Creating company - Current user: " + (currentUser != null ? currentUser.getEmail() + " (" + currentUser.getRole() + ")" : "null"));
        
        if (currentUser == null || currentUser.getRole() != User.Role.DEVELOPER) {
            return ResponseEntity.status(403).body("Only developers can create companies");
        }

        // Set developer reference and default status
        company.setDeveloper(currentUser);
        company.setStatus("active");

        // Optional: Set default max limits if not provided
        if (company.getMaxUsers() == null) company.setMaxUsers(10);
        if (company.getMaxAdmins() == null) company.setMaxAdmins(2);

        // Optional validation
        if (company.getMaxUsers() < 0) {
            return ResponseEntity.badRequest().body("maxUsers must be >= 0");
        }
        if (company.getMaxAdmins() < 0) {
            return ResponseEntity.badRequest().body("maxAdmins must be >= 0");
        }

        System.out.println("💾 Saving company: " + company.getName());
        Company savedCompany = companyDao.save(company);
        System.out.println("✅ Company saved with ID: " + savedCompany.getId());
        return ResponseEntity.ok(savedCompany);
    }

    // ✅ 2. Get All Companies (Admin Purpose)
    @GetMapping("/all")
    public ResponseEntity<?> getAllCompanies() {
        User currentUser = SecurityUtil.getCurrentUser();
        System.out.println("🔍 Current user: " + (currentUser != null ? currentUser.getEmail() + " (" + currentUser.getRole() + ")" : "null"));
        
        if (currentUser == null || currentUser.getRole() != User.Role.DEVELOPER) {
            return ResponseEntity.status(403).body("Only developers can view all companies");
        }
        
        List<Company> companies = companyDao.findAll();
        System.out.println("📊 Found " + companies.size() + " companies");
        return ResponseEntity.ok(companies);
    }

    // ✅ 3. Get Companies created by current Developer
    @GetMapping("/my")
    public ResponseEntity<?> getMyCompanies() {
        User developer = SecurityUtil.getCurrentUser();
        if (developer == null || developer.getRole() != User.Role.DEVELOPER) {
            return ResponseEntity.status(403).body("Access denied");
        }
        List<Company> companies = companyDao.findByDeveloper(developer);
        return ResponseEntity.ok(companies);
    }

    // ✅ 4. Delete Company by ID
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteCompany(@PathVariable Long id) {
        companyDao.delete(id);
        return ResponseEntity.ok("Company deleted successfully!");
    }

    // ✅ 5. Revoke Company by ID (set status to inactive)
    @PutMapping("/revoke/{id}")
    public ResponseEntity<String> revokeCompany(@PathVariable Long id) {
        Company company = companyDao.findById(id);
        if (company != null) {
            company.setStatus("inactive");
            companyDao.update(company);
            return ResponseEntity.ok("Company revoked successfully!");
        } else {
            return ResponseEntity.badRequest().body("Company not found!");
        }
    }

    // ✅ 6. Unrevoke Company (set status to active)
    @PutMapping("/unrevoke/{id}")
    public ResponseEntity<String> unrevokeCompany(@PathVariable Long id) {
        Company company = companyDao.findById(id);
        if (company != null) {
            company.setStatus("active");
            companyDao.update(company);
            return ResponseEntity.ok("Company activated successfully!");
        } else {
            return ResponseEntity.badRequest().body("Company not found!");
        }
    }

    // ✅ 7. Update Company by ID
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateCompany(@PathVariable Long id, @RequestBody Company updatedCompany) {
        // Get current user (should be developer)
        User currentUser = SecurityUtil.getCurrentUser();
        System.out.println("🔍 Updating company - Current user: " + (currentUser != null ? currentUser.getEmail() + " (" + currentUser.getRole() + ")" : "null"));
        
        if (currentUser == null || currentUser.getRole() != User.Role.DEVELOPER) {
            return ResponseEntity.status(403).body("Only developers can update companies");
        }

        // Find existing company
        Company existingCompany = companyDao.findById(id);
        if (existingCompany == null) {
            return ResponseEntity.status(404).body("Company not found");
        }

        // Validate input
        if (updatedCompany.getMaxUsers() != null && updatedCompany.getMaxUsers() < 0) {
            return ResponseEntity.badRequest().body("maxUsers must be >= 0");
        }
        if (updatedCompany.getMaxAdmins() != null && updatedCompany.getMaxAdmins() < 0) {
            return ResponseEntity.badRequest().body("maxAdmins must be >= 0");
        }

        // Update fields if provided
        if (updatedCompany.getName() != null && !updatedCompany.getName().trim().isEmpty()) {
            existingCompany.setName(updatedCompany.getName().trim());
        }
        if (updatedCompany.getEmail() != null && !updatedCompany.getEmail().trim().isEmpty()) {
            existingCompany.setEmail(updatedCompany.getEmail().trim());
        }
        if (updatedCompany.getPhone() != null && !updatedCompany.getPhone().trim().isEmpty()) {
            existingCompany.setPhone(updatedCompany.getPhone().trim());
        }
        if (updatedCompany.getMaxUsers() != null) {
            existingCompany.setMaxUsers(updatedCompany.getMaxUsers());
        }
        if (updatedCompany.getMaxAdmins() != null) {
            existingCompany.setMaxAdmins(updatedCompany.getMaxAdmins());
        }

        System.out.println("💾 Updating company: " + existingCompany.getName());
        Company savedCompany = companyDao.update(existingCompany);
        System.out.println("✅ Company updated with ID: " + savedCompany.getId());
        return ResponseEntity.ok(savedCompany);
    }

    // ✅ 8. Get Company Name by ID
    @GetMapping("/name/{id}")
    public ResponseEntity<String> getCompanyNameById(@PathVariable Long id) {
        Company company = companyDao.findById(id);
        if (company != null) {
            return ResponseEntity.ok(company.getName());
        } else {
            return ResponseEntity.status(404).body("Company not found");
        }
    }

    // ✅ 9. Get Total Companies Count (For DEVELOPER dashboard)
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCompaniesCount() {
        try {
            long totalCompanies = companyDao.getTotalCount();
            return ResponseEntity.ok(totalCompanies);
        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }
}
