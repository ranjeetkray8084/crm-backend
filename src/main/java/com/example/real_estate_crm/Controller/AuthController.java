package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.UserAvatar;
import com.example.real_estate_crm.repository.UserAvatarRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.security.JwtUtil;
import com.example.real_estate_crm.service.dao.UserDao;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
    "https://leadstracker.in",
    "https://www.leadstracker.in", 
    "https://crm.leadstracker.in",
    "https://test.leadstracker.in",
    "http://localhost:5173"
}, allowCredentials = "true")
public class AuthController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserAvatarRepository userAvatarRepository;


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // ✅ Login Endpoint
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody User user, HttpServletResponse response) {
        Optional<User> optionalUser = userRepository.findByEmail(user.getEmail());

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "This email is not in record."));
        }

        User dbUser = optionalUser.get();

        if (Boolean.FALSE.equals(dbUser.getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "Your account has been deactivated. Please contact Admin."));
        }

        if (dbUser.getCompany() != null && "inactive".equalsIgnoreCase(dbUser.getCompany().getStatus())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Collections.singletonMap("message", "Your company access has been revoked. Please contact support."));
        }

        if (!passwordEncoder.matches(user.getPassword(), dbUser.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Password is wrong."));
        }

        // ✅ Generate access token valid for 7 days (no refresh logic)
        String accessToken = jwtUtil.generateToken(dbUser.getEmail());

        // ✅ Set access token in secure HttpOnly cookie
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", accessToken)
                .httpOnly(true)
                .secure(true)  // Use false for localhost if needed
                .path("/")
                .maxAge(60 * 60 * 24 * 7)  // 7 days
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        
        UserAvatar userAvatar = userAvatarRepository.findFirstByUserIdOrderByIdDesc(dbUser.getUserId());
        String avatarName = userAvatar != null ? userAvatar.getAvatarName() : null;

        // ✅ Also return access token in body if needed by client (e.g., mobile apps)
     // ✅ Also return access token in body if needed by client (e.g., mobile apps)
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("message", "Login successful");
        responseBody.put("accessToken", accessToken);
        responseBody.put("userId", dbUser.getUserId());
        responseBody.put("email", dbUser.getEmail());
        responseBody.put("name", dbUser.getName());
        responseBody.put("role", dbUser.getRole().name());
        responseBody.put("avatar", avatarName); // ✅ Add avatar from UserAvatar
        responseBody.put("companyId", dbUser.getCompany() != null ? dbUser.getCompany().getId() : null);
        responseBody.put("companyName", dbUser.getCompany() != null ? dbUser.getCompany().getName() : null);

        return ResponseEntity.ok(responseBody);


    }


    // 🔄 Removed refresh-token endpoint (no longer needed)

    // ✅ Send OTP
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, String>> sendOtp(@RequestParam String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Collections.singletonMap("error", "This email is not registered."));
        }

        userDao.sendResetPasswordEmail(email);
        return ResponseEntity.ok(Collections.singletonMap("message", "OTP sent to your email."));
    }

    // ✅ Verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        boolean isValid = userDao.verifyOtp(email, otp);
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        response.put("message", isValid ? "OTP verified" : "Invalid or expired OTP");
        return ResponseEntity.ok(response);
    }

    // ✅ Reset Password
    @PostMapping("/reset-password-with-otp")
    public ResponseEntity<Map<String, String>> resetPasswordWithOtp(
            @RequestParam String email,
            @RequestParam String newPassword) {
        userDao.resetPasswordWithOtp(email, newPassword);
        return ResponseEntity.ok(Collections.singletonMap("message", "Password reset successfully"));
    }

    // ✅ Logout and clear cookies
    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie deleteCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());

        return ResponseEntity.ok(Collections.singletonMap("message", "Logged out successfully."));
    }

    // 🔍 Debug endpoint to validate token
    @GetMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        Map<String, Object> response = new HashMap<>();
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.put("valid", false);
            response.put("error", "No Bearer token provided");
            return ResponseEntity.ok(response);
        }
        
        String token = authHeader.substring(7);
        
        try {
            String email = jwtUtil.extractEmail(token);
            boolean isExpired = jwtUtil.isTokenExpired(token);
            boolean isValid = jwtUtil.isTokenValid(token);
            
            response.put("valid", isValid && !isExpired);
            response.put("email", email);
            response.put("expired", isExpired);
            response.put("tokenValid", isValid);
            
            if (email != null) {
                Optional<User> user = userRepository.findByEmail(email);
                response.put("userExists", user.isPresent());
                if (user.isPresent()) {
                    response.put("userActive", user.get().getStatus());
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("valid", false);
            response.put("error", e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

}
