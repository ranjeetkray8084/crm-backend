package com.example.real_estate_crm.service.dao;

import java.util.List;
import java.util.Optional;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;

public interface UserDao {

    List<User> getAllUsers();

    Optional<User> getUserById(Long id);
    Optional<User> findById(Long id);

    User save(User user);

    User updateUser(User user);

    void deleteById(Long id);

    Optional<User> findByEmail(String email);

    void logout(Long userId);

    User authenticateUser(User user); // Authenticate user (Login)

    void sendResetPasswordEmail(String email); // Send OTP to email

    boolean verifyOtp(String email, String otp); // Verify OTP for given email

    void resetPasswordWithOtp(String email, String newPassword); // Reset password after OTP verification

    Optional<String> findUsernameByUserId(Long userId);

    // 🔍 New: Get all users in a company
    List<User> findUsersByCompanyId(Long companyId);

    // 🔒 Revoke / Unrevoke user
    boolean revokeUser(Long userId);
    boolean unRevokeUser(Long userId);

    // ✅ NEW METHODS (admin-user structure support):

    // 🔽 Get all users under a specific admin
    List<User> findUsersByAdminId(Long adminId);

    // 🔐 Secure fetch: check if user belongs to admin
    Optional<User> findUserByIdAndAdminId(Long userId, Long adminId);

    // 🎯 Get all admins created by Director (admin without parent admin)
    List<User> findAdminsByCompanyId(Long companyId);

    // ✅ Correct return type
    Company findCompanyById(Long id);

	User findDirectorByCompany(Company company);
}
