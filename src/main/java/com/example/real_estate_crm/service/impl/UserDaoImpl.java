package com.example.real_estate_crm.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.UserDao;

import jakarta.mail.internet.MimeMessage;

@Repository
public class UserDaoImpl implements UserDao {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository; // ✅ Needed for findCompanyById

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

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
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (userRepository.existsByPhone(user.getPhone())) {
            throw new IllegalArgumentException("Phone number already exists");
        }

        if (user.getPassword() != null && !user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        if (user.getStatus() == null) {
            user.setStatus(true);
        }

        if (user.getCompany() == null || user.getCompany().getId() == null) {
            throw new IllegalArgumentException("Company is required");
        }

        if (user.getRole() == User.Role.USER && user.getAdmin() == null) {
            throw new IllegalArgumentException("Admin must be assigned for USER role");
        }

        return userRepository.save(user);
    }

    
    @Override
    public User findDirectorByCompany(Company company) {
        try {
            return userRepository.findByCompanyAndRole(company, User.Role.DIRECTOR);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public User updateUser(User user) {
        User existingUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + user.getUserId()));

        // Check if email is being changed and if it already exists for another user
        if (!existingUser.getEmail().equals(user.getEmail())) {
            Optional<User> userWithEmail = userRepository.findByEmailAndUserIdNot(user.getEmail(), user.getUserId());
            if (userWithEmail.isPresent()) {
                throw new IllegalArgumentException("Email already exists");
            }
        }

        // Check if phone is being changed and if it already exists for another user
        if (!existingUser.getPhone().equals(user.getPhone())) {
            Optional<User> userWithPhone = userRepository.findByPhoneAndUserIdNot(user.getPhone(), user.getUserId());
            if (userWithPhone.isPresent()) {
                throw new IllegalArgumentException("Phone number already exists");
            }
        }

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());

        return userRepository.save(existingUser);
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
        // User logout logic if needed
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
    public void sendResetPasswordEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Email not found. Please register or check your email.");
        }

        User user = userOpt.get();
        String otp = String.valueOf(new Random().nextInt(900000) + 100000);
        user.setOtpCode(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            // ✅ RFC 5322 required headers
            helper.setFrom("support@smartprocares.com", "SmartProCare Support"); 
            helper.setTo(email);
            helper.setSubject("🔐 Your OTP for Password Reset");
            helper.setReplyTo("support@smartprocares.com");

            // ✅ HTML email template
         // ✅ HTML email template (Dark CRM theme)
            String content = """
                <html>
                  <body style="font-family: Arial, sans-serif; line-height: 1.6; background-color: #1e1e2f; color: #ffffff; padding: 20px; border-radius: 8px;">
                    <div style="max-width: 600px; margin: auto; background-color: #2b2b3c; padding: 20px; border-radius: 10px;">
                      
                      <h2 style="color: #4cafef; text-align: center;">Hello %s,</h2>
                      <p style="font-size: 15px; color: #dddddd;">We received a request to reset your password.</p>
                      
                      <p style="font-size: 16px; margin-top: 20px;">
                        👉 <strong>Your One-Time Password (OTP) is:</strong><br/>
                        <span style="display: inline-block; margin-top: 10px; font-size: 22px; color: #ff6b6b; background: #1e1e2f; padding: 10px 20px; border-radius: 6px; font-weight: bold;">
                          %s
                        </span>
                      </p>

                      <p style="margin-top: 20px; font-size: 14px; color: #bbbbbb;">
                        This OTP is valid for <strong>5 minutes</strong>. Please do not share it with anyone.
                      </p>

                      <p style="font-size: 14px; color: #999999; margin-top: 30px;">
                        If you did not request a password reset, you can ignore this message.
                      </p>

                      <hr style="margin: 30px 0; border: 0; border-top: 1px solid #444;" />
                      
                      <p style="font-size: 14px; color: #888; text-align: center;">
                        Best regards,<br/>SmartProCare Team
                      </p>
                    </div>
                  </body>
                </html>
            """.formatted(user.getName(), otp);


            helper.setText(content, true); // true = HTML

            mailSender.send(mimeMessage);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send reset password email", e);
        }
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
            user.setOtpCode(null);
            user.setOtpExpiry(null);
            userRepository.save(user);
        }
    }

    @Override
    public List<User> findUsersByCompanyId(Long companyId) {
        return userRepository.findByCompany_Id(companyId);
    }

    @Override
    public boolean revokeUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(false);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public boolean unRevokeUser(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setStatus(true);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public List<User> findUsersByAdminId(Long adminId) {
        return userRepository.findByAdmin_UserId(adminId);
    }

    @Override
    public Optional<User> findUserByIdAndAdminId(Long userId, Long adminId) {
        return userRepository.findByUserIdAndAdmin_UserId(userId, adminId);
    }

    @Override
    public List<User> findAdminsByCompanyId(Long companyId) {
        return userRepository.findByCompany_IdAndRoleAndAdminIsNull(companyId, User.Role.ADMIN);
    }

    // ✅ NEW: Get company object by ID
    @Override
    public Company findCompanyById(Long id) {
        return companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found with id: " + id));
    }

    @Override
    public Optional<String> findUsernameByUserId(Long userId) {
        return userRepository.findById(userId).map(User::getName);
    }
}
