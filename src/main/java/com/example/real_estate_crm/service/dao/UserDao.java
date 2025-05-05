package com.example.real_estate_crm.service.dao;

import java.util.List;
import java.util.Optional;

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

    
}
