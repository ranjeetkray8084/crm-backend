package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.security.SecurityUtil;
import com.example.real_estate_crm.service.dao.CompanyDao;
import com.example.real_estate_crm.service.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyDao companyDao;
    private final UserDao userDao;

    @Autowired
    public CompanyController(CompanyDao companyDao, UserDao userDao) {
        this.companyDao = companyDao;
        this.userDao = userDao;
    }

    // ✅ 1. Add New Company (Only Developer)
    @PostMapping("/add")
    public ResponseEntity<?> addCompany(@RequestBody Company company) {
 

        // Set developer reference and default status
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

        Company savedCompany = companyDao.save(company);
        return ResponseEntity.ok(savedCompany);
    }

    // ✅ 2. Get All Companies (Admin Purpose)
    @GetMapping("/all")
    public ResponseEntity<List<Company>> getAllCompanies() {
        List<Company> companies = companyDao.findAll();
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

    // ✅ 7. Get Company Name by ID
    @GetMapping("/name/{id}")
    public ResponseEntity<String> getCompanyNameById(@PathVariable Long id) {
        Company company = companyDao.findById(id);
        if (company != null) {
            return ResponseEntity.ok(company.getName());
        } else {
            return ResponseEntity.status(404).body("Company not found");
        }
    }
}
