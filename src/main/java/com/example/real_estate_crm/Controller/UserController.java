package com.example.real_estate_crm.Controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.real_estate_crm.model.UserAvatar;
import com.example.real_estate_crm.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.real_estate_crm.model.User.Role; // ✅ Correct import
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.service.EmailService;
import com.example.real_estate_crm.service.dao.UserAvatarDao;
import com.example.real_estate_crm.service.dao.UserDao;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserAvatarDao userAvatarDao;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public List<User> getAllUsers() {
        return userDao.getAllUsers();
    }
    
    @GetMapping("/{id}/username")
    public ResponseEntity<String> getUsernameById(@PathVariable Long id) {
        Optional<User> user = userDao.getUserById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get().getName());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }
    
    @GetMapping("/user-role")
    public List<User> getAllUsersWithUserRole() {
        return userRepository.findByRole(Role.USER);
    }
    
    @GetMapping("/api/users/role/{role}")
    public List<User> getUsersByRole(@PathVariable Role role) {
        return userRepository.findByRole(role); // Role enum is automatically matched
    }




    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userDao.getUserById(id);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User savedUser = userDao.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error creating user: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            Optional<User> optionalUser = userDao.findById(id);
            if (optionalUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
            }

            User existingUser = optionalUser.get();
            existingUser.setName(user.getName());
            existingUser.setEmail(user.getEmail());
            existingUser.setPhone(user.getPhone());
            existingUser.setRole(user.getRole());

            if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
                existingUser.setPassword(user.getPassword());
            }

            userDao.updateUser(existingUser);
            return ResponseEntity.ok(existingUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userDao.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User user) {
        User authenticatedUser = userDao.authenticateUser(user);

        if (authenticatedUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("userId", authenticatedUser.getUserId());
            response.put("email", authenticatedUser.getEmail());
            response.put("name", authenticatedUser.getName());
            response.put("role", authenticatedUser.getRole().name());

            return ResponseEntity.ok(response);
        }

        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("message", "Invalid email or password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestParam String email) {
        try {
            // Check if email exists
            Optional<User> userOpt = userRepository.findByEmail(email); // assuming you have this method
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "This email is not registered."));
            }

            // Send OTP if email exists
            userDao.sendResetPasswordEmail(email);
            return ResponseEntity.ok(Collections.singletonMap("message", "OTP sent to your email."));
            
        } catch (Exception e) {
            return ResponseEntity
                    .badRequest()
                    .body(Collections.singletonMap("error", "An error occurred while sending OTP."));
        }
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        boolean isValid = userDao.verifyOtp(email, otp);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("message", isValid ? "OTP verified" : "Invalid or expired OTP");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password-with-otp")
    public ResponseEntity<Map<String, String>> resetPasswordWithOtp(
            @RequestParam String email,
            @RequestParam String newPassword) {
        userDao.resetPasswordWithOtp(email, newPassword);
        return ResponseEntity.ok(Collections.singletonMap("message", "Password reset successfully"));
    }

    @PostMapping("/{id}/upload-avatar")
    public ResponseEntity<String> uploadAvatar(@PathVariable Long id,
                                               @RequestParam("avatar") MultipartFile file,
                                               @RequestParam("avatarName") String avatarName) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No file uploaded");
        }

        try {
            byte[] imageBytes = file.getBytes();
            userAvatarDao.saveUserAvatar(id, imageBytes, avatarName);
            return ResponseEntity.ok("Avatar uploaded successfully!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error uploading avatar");
        }
    }

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable("id") Long userId) {
        UserAvatar avatar = userAvatarDao.getUserAvatarByUserId(userId);

        if (avatar == null || avatar.getImage() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(avatar.getImage().length);
        headers.set("Content-Disposition", "inline; filename=\"" + avatar.getAvatarName() + "\"");

        return new ResponseEntity<>(avatar.getImage(), headers, HttpStatus.OK);
    }
}
