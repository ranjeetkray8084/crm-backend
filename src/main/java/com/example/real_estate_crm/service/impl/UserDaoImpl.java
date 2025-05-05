package com.example.real_estate_crm.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.UserDao;
import org.springframework.mail.javamail.JavaMailSender;



import java.util.Optional;
import java.util.Random;

@Repository
public class UserDaoImpl implements UserDao {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JavaMailSender mailSender;

    // Remove mailSender if not used — or @Autowired it properly if needed
    // @Autowired
    // private JavaMailSender mailSender;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }
    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public User save(User user) {
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        return userRepository.save(user);
    }
    
    @Override
    public Optional<String> findUsernameByUserId(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            return Optional.of(userOpt.get().getName());
        }
        return Optional.empty();
    }


    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + user.getUserId()));

        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(existingUser.getPassword());
        }

        return userRepository.save(user);
    }
    
    @Override
    public User authenticateUser(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent() &&
            passwordEncoder.matches(user.getPassword(), existingUser.get().getPassword())) {
            return existingUser.get();
        }
        return null;
    }

    @Override
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public void logout(Long userId) {
        System.out.println("User logged out: " + userId);
        // Consider implementing token invalidation if using JWT/session
    }
    
    @Override
    public void sendResetPasswordEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email not found. Please register or check your email.");
        }

        User user = userOpt.get();

        // Generate 6-digit OTP
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        // Create and send a friendlier, more readable email
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("🔐 Your OTP for Camera Booking Password Reset");

        String content = String.format("""
                Hi %s,

                We received a request to reset your password.

                👉 Your One-Time Password (OTP) is: %s

                This OTP is valid for  5 minutes. Please do not share it with anyone.

                If you did not request a password reset, you can ignore this message.

                Thanks,
                Camera Booking Support Team
                """, user.getName(), otp);

        message.setText(content);

        mailSender.send(message);
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return user.getOtpCode() != null &&
                   user.getOtpCode().equals(otp) &&
                   user.getOtpExpiry() != null &&
                   user.getOtpExpiry().isAfter(LocalDateTime.now());
        }
        return false;
    }

    @Override
    public void resetPasswordWithOtp(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setOtpCode(null); // Clear OTP after use
            user.setOtpExpiry(null);
            userRepository.save(user);
        }
    }


}
