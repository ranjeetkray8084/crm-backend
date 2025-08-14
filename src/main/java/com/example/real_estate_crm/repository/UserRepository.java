package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.User.Role;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 🔐 Authentication
    Optional<User> findByEmail(String email);


    User findByEmailAndPassword(String email, String password);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // 🔍 Basic role-based queries
    List<User> findByRole(Role role);

    List<User> findByCompanyIdAndRole(Long companyId, Role role);

    List<User> findByCompany_Id(Long companyId);

    // 🔍 Admin-user relationship
    List<User> findByAdmin_UserId(Long adminId);

    List<User> findByCompanyIdAndAdmin_UserId(Long companyId, Long adminId);

    Optional<User> findByUserIdAndAdmin_UserId(Long userId, Long adminId);

    // 🔍 Director-level queries (admins with no admin)
    List<User> findByCompany_IdAndRoleAndAdminIsNull(Long companyId, Role role);

    // 🔢 Count users by admin & company
    @Query("SELECT COUNT(u) FROM User u WHERE u.admin.userId = :adminId AND u.company.id = :companyId")
    long countUsersByAdminAndCompany(@Param("adminId") Long adminId, @Param("companyId") Long companyId);

    // ✅ FIXED: Count by admin, company and status (with custom query)
    @Query("SELECT COUNT(u) FROM User u WHERE u.admin.userId = :adminId AND u.company.id = :companyId AND u.status = :status")
    long countUsersByAdminAndCompanyAndStatus(@Param("adminId") Long adminId,
            @Param("companyId") Long companyId,
            @Param("status") boolean status);

    // 🔢 Role and status-based counts
    long countByCompany_Id(Long companyId); // total users

    long countByCompany_IdAndRole(Long companyId, Role role); // total by role

    long countByCompany_IdAndRoleAndStatusTrue(Long companyId, Role role); // active by role

    long countByCompany_IdAndRoleAndStatusFalse(Long companyId, Role role); // deactive by role

    // 🔢 Overall status counts
    long countByCompany_IdAndStatusTrue(Long companyId); // active users

    long countByCompany_IdAndStatusFalse(Long companyId); // deactive users

    // 🔍 Director user fetch
    long countByCompanyAndRole(Company company, Role role);

    User findByCompanyAndRole(Company company, Role role);

    // 🔢 Global counts for DEVELOPER dashboard
    long countByRole(Role role);

    // 🔍 Find user by phone excluding specific user ID
    Optional<User> findByPhoneAndUserIdNot(String phone, Long userId);

    // 🔍 Find user by email excluding specific user ID  
    Optional<User> findByEmailAndUserIdNot(String email, Long userId);
}
