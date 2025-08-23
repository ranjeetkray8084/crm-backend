package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.UserDTO;
import com.example.real_estate_crm.dto.CreateUserRequest;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.UserAvatar;
import com.example.real_estate_crm.model.User.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.security.JwtUtil;
import com.example.real_estate_crm.service.dao.UserAvatarDao;
import com.example.real_estate_crm.service.dao.UserDao;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserDao userDao;

    @Autowired
    private UserAvatarDao userAvatarDao;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/{id}/username")
    public ResponseEntity<String> getUsernameById(@PathVariable Long id) {
        Optional<User> user = userDao.getUserById(id);
        return user.map(value -> ResponseEntity.ok(value.getName()))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found"));
    }

    @GetMapping("/user-role")
    public List<UserDTO> getAllUsersWithUserRole() {
        List<User> users = userRepository.findByRole(Role.USER);
        return users.stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/director-role")
    public List<User> getAllUsersWithDirectorRole() {
        return userRepository.findByRole(Role.DIRECTOR);
    }

    @GetMapping("/admin-role")
    public List<User> getAllUsersWithAdminRole() {
        return userRepository.findByRole(Role.ADMIN);
    }

    @GetMapping("/role/{role}")
    public List<User> getUsersByRole(@PathVariable Role role) {
        return userRepository.findByRole(role);
    }

    @GetMapping("/all-users/{companyId}")
    public List<UserDTO> getAllUsers(@PathVariable Long companyId) {
        return userRepository.findByCompany_Id(companyId).stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/user-role/{companyId}")
    public List<UserDTO> getAllUsersWithUserRole(@PathVariable Long companyId) {
        List<User> users = userRepository.findByCompanyIdAndRole(companyId, Role.USER);
        return users.stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/admin-role/{companyId}")
    public List<UserDTO> getAllUsersWithAdminRole(@PathVariable Long companyId) {
        return userRepository.findByCompanyIdAndRole(companyId, Role.ADMIN).stream().map(UserDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/role/{companyId}/{role}")
    public List<UserDTO> getUsersByRole(@PathVariable Long companyId, @PathVariable Role role) {
        return userRepository.findByCompanyIdAndRole(companyId, role).stream().map(UserDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userDao.getUserById(id);
            if (user.isPresent()) {
                return ResponseEntity.ok(new UserDTO(user.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> requestMap) {
        try {
            // Convert Map to User object manually to avoid JSON deserialization issues
            User user = new User();
            
            // Set basic fields
            if (requestMap.containsKey("name")) {
                user.setName((String) requestMap.get("name"));
            }
            if (requestMap.containsKey("email")) {
                user.setEmail((String) requestMap.get("email"));
            }
            if (requestMap.containsKey("phone")) {
                user.setPhone((String) requestMap.get("phone"));
            }
            if (requestMap.containsKey("password")) {
                user.setPassword((String) requestMap.get("password"));
            }
            if (requestMap.containsKey("role")) {
                String roleStr = (String) requestMap.get("role");
                user.setRole(Role.valueOf(roleStr));
            }
            
            // Handle company
            Long companyId = null;
            if (requestMap.containsKey("company")) {
                Map<String, Object> companyMap = (Map<String, Object>) requestMap.get("company");
                if (companyMap.containsKey("id")) {
                    companyId = Long.valueOf(companyMap.get("id").toString());
                }
            }
            
            if (companyId == null) {
                return ResponseEntity.badRequest().body("Company ID is required");
            }
            
            // Check if company exists
            Optional<Company> optionalCompany = Optional.ofNullable(userDao.findCompanyById(companyId));
            if (optionalCompany.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid company ID");
            }
            
            Company company = optionalCompany.get();
            user.setCompany(company);
            
            // Handle admin assignment for USER role
            if (user.getRole() == Role.USER && requestMap.containsKey("admin")) {
                Object adminObj = requestMap.get("admin");
                if (adminObj != null && adminObj instanceof Map) {
                    Map<String, Object> adminMap = (Map<String, Object>) adminObj;
                    if (adminMap.containsKey("userId")) {
                        Long adminId = Long.valueOf(adminMap.get("userId").toString());
                        Optional<User> adminUser = userDao.findById(adminId);
                        if (adminUser.isPresent() && adminUser.get().getRole() == Role.ADMIN) {
                            user.setAdmin(adminUser.get());
                        } else {
                            return ResponseEntity.badRequest().body("Invalid admin ID or admin not found");
                        }
                    }
                }
                // If admin is null or not a valid map, user will be created without admin assignment
            }
            
            // Set default status
            user.setStatus(true);
            
            // Limit checks - using company ID instead of company object to avoid JOIN issues
            long existingCount = userRepository.countByCompany_IdAndRole(companyId, user.getRole());

            if (user.getRole() == Role.USER && company.getMaxUsers() != null && existingCount >= company.getMaxUsers()) {
                return ResponseEntity.badRequest().body("❌ User creation failed: Max user limit reached for this company.");
            }

            if (user.getRole() == Role.ADMIN && company.getMaxAdmins() != null && existingCount >= company.getMaxAdmins()) {
                return ResponseEntity.badRequest().body("❌ Admin creation failed: Max admin limit reached for this company.");
            }

            // Save user
            User savedUser = userDao.save(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(new UserDTO(savedUser));

        } catch (IllegalArgumentException e) {
            // Handle validation errors from UserDao
            return ResponseEntity.badRequest().body("❌ " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // For debug in logs
            return ResponseEntity.badRequest().body("❌ Error creating user: " + e.getMessage());
        }
    }

    
    @PutMapping("/update-profile/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user, HttpServletResponse response) {
        Optional<User> optionalUser = userDao.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User existingUser = optionalUser.get();

        boolean emailChanged = !existingUser.getEmail().equals(user.getEmail());

        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());

        try {
            User updatedUser = userDao.updateUser(existingUser);
            System.out.println("✅ User updated successfully: " + updatedUser.getEmail());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("message", "User updated successfully");
            responseBody.put("user", new UserDTO(updatedUser));

            // If email changed, generate new JWT token
            if (emailChanged) {
                System.out.println("📧 Email changed, generating new JWT token");
                String newToken = jwtUtil.generateToken(updatedUser.getEmail());

                // Set new token in cookie
                ResponseCookie accessCookie = ResponseCookie.from("accessToken", newToken)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(60 * 60 * 24 * 7) // 7 days
                        .sameSite("Strict")
                        .build();

                response.setHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

                // Also return new token in response body
                responseBody.put("newToken", newToken);
                responseBody.put("tokenUpdated", true);
            }

            return ResponseEntity.ok(responseBody);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Validation error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update user: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userDao.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/upload-avatar")
    public ResponseEntity<String> uploadAvatar(@PathVariable Long id,
            @RequestParam("avatar") MultipartFile file,
            @RequestParam("avatarName") String avatarName) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
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
    public ResponseEntity<byte[]> getUserAvatar(@PathVariable Long id) {
        UserAvatar avatar = userAvatarDao.getUserAvatarByUserId(id);

        if (avatar == null || avatar.getImage() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(avatar.getImage().length);
        headers.setContentDisposition(ContentDisposition.inline().filename(avatar.getAvatarName()).build());

        return new ResponseEntity<>(avatar.getImage(), headers, HttpStatus.OK);
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UserDTO>> getUsersByCompanyId(@PathVariable Long companyId) {
        List<UserDTO> users = userRepository.findByCompany_Id(companyId).stream().map(UserDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("jwt", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("Strict")
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(Collections.singletonMap("message", "Logged out successfully"));
    }

    @PutMapping("/{id}/revoke")
    public ResponseEntity<String> revokeUser(@PathVariable("id") Long userId) {
        boolean success = userDao.revokeUser(userId);
        if (success) {
            return ResponseEntity.ok("User revoked (set to inactive) successfully.");
        } else {
            return ResponseEntity.badRequest().body("User not found.");
        }
    }

    @PutMapping("/{id}/unrevoke")
    public ResponseEntity<String> unrevokeUser(@PathVariable("id") Long userId) {
        boolean success = userDao.unRevokeUser(userId);
        if (success) {
            return ResponseEntity.ok("User unrevoked (set to active) successfully.");
        } else {
            return ResponseEntity.badRequest().body("User not found.");
        }
    }

    @GetMapping("/admin/{adminId}/users")
    public ResponseEntity<List<UserDTO>> getUsersByAdminId(@PathVariable Long adminId) {
        List<UserDTO> users = userDao.findUsersByAdminId(adminId).stream().map(UserDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @GetMapping("/admin/{adminId}/user/{userId}")
    public ResponseEntity<?> getUserIfBelongsToAdmin(@PathVariable Long adminId, @PathVariable Long userId) {
        Optional<User> userOpt = userDao.findUserByIdAndAdminId(userId, adminId);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(new UserDTO(userOpt.get()));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User doesn't belong to the given admin.");
        }
    }

    @GetMapping("/company/{companyId}/admins")
    public ResponseEntity<List<UserDTO>> getAdminsByCompany(@PathVariable Long companyId) {
        List<UserDTO> admins = userDao.findAdminsByCompanyId(companyId).stream().map(UserDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/company/{companyId}/users")
    public ResponseEntity<List<UserDTO>> getUsersByCompany(@PathVariable Long companyId) {
        List<UserDTO> users = userDao.findUsersByCompanyId(companyId).stream().map(UserDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}/assign-admin")
    public ResponseEntity<?> assignAdmin(@PathVariable Long userId, @RequestBody Map<String, Long> body) {
        Long adminId = body.get("adminId");
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<User> adminOpt = userRepository.findById(adminId);

        if (userOpt.isPresent() && adminOpt.isPresent()) {
            User user = userOpt.get();
            user.setAdmin(adminOpt.get());
            userRepository.save(user);
            return ResponseEntity.ok("User assigned successfully");
        }
        return ResponseEntity.badRequest().body("Invalid user/admin ID");
    }

    @PutMapping("/{userId}/unassign-admin")
    public ResponseEntity<?> unassignAdmin(@PathVariable Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = optionalUser.get();
        user.setAdmin(null);
        userRepository.save(user);

        return ResponseEntity.ok("User unassigned successfully.");
    }

    @GetMapping("/count-by-admin/{adminId}")
    public ResponseEntity<Long> countUsersByAdmin(@PathVariable Long adminId, @RequestParam Long companyId) {
        long count = userRepository.countUsersByAdminAndCompany(adminId, companyId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(@CookieValue(value = "accessToken", required = false) String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Session expired. Please login again."));
        }

        try {
            String email = jwtUtil.validateTokenAndRetrieveSubject(accessToken);
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Collections.singletonMap("message", "Invalid user."));
            }

            return ResponseEntity.ok(Collections.singletonMap("message", "Session active"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Collections.singletonMap("message", "Session expired or invalid. Please login again."));
        }
    }

    @GetMapping("/count-summary/{companyId}/{userId}")
    public ResponseEntity<Map<String, Long>> getUserCountSummaryByRole(
            @PathVariable Long companyId,
            @PathVariable Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        User user = optionalUser.get();
        Role role = user.getRole();
        Map<String, Long> response = new HashMap<>();

        if (role == Role.DIRECTOR) {
            long totalUsers = userRepository.countByCompany_Id(companyId);
            long totaldirector = userRepository.countByCompany_IdAndRole(companyId, Role.DIRECTOR);
            totalUsers = totalUsers - totaldirector;

            // ADMIN role
            long totalAdmins = userRepository.countByCompany_IdAndRole(companyId, Role.ADMIN);
            long activeAdmins = userRepository.countByCompany_IdAndRoleAndStatusTrue(companyId, Role.ADMIN);
            long deactiveAdmins = userRepository.countByCompany_IdAndRoleAndStatusFalse(companyId, Role.ADMIN);

            // USER role
            long totalNormalUsers = userRepository.countByCompany_IdAndRole(companyId, Role.USER);
            long activeNormalUsers = userRepository.countByCompany_IdAndRoleAndStatusTrue(companyId, Role.USER);
            long deactiveNormalUsers = userRepository.countByCompany_IdAndRoleAndStatusFalse(companyId, Role.USER);

            response.put("totalUsers", totalUsers);
            response.put("totalAdmins", totalAdmins);
            response.put("activeAdmins", activeAdmins);
            response.put("deactiveAdmins", deactiveAdmins);
            response.put("totalNormalUsers", totalNormalUsers);
            response.put("activeNormalUsers", activeNormalUsers);
            response.put("deactiveNormalUsers", deactiveNormalUsers);

        } else if (role == Role.ADMIN) {
            long totalAssignedUsers = userRepository.countUsersByAdminAndCompany(userId, companyId);
            long activeAssignedUsers = userRepository.countUsersByAdminAndCompanyAndStatus(userId, companyId, true);
            long deactiveAssignedUsers = userRepository.countUsersByAdminAndCompanyAndStatus(userId, companyId, false);

            response.put("totalAssignedUsers", totalAssignedUsers);
            response.put("activeAssignedUsers", activeAssignedUsers);
            response.put("deactiveAssignedUsers", deactiveAssignedUsers);
        }

        return ResponseEntity.ok(response);
    }

    // ✅ Developer Dashboard Endpoints

    @GetMapping("/total-count")
    public ResponseEntity<Long> getTotalUsersCount() {
        try {
            long totalUsers = userRepository.count();
            return ResponseEntity.ok(totalUsers);
        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }

    @GetMapping("/admins/total-count")
    public ResponseEntity<Long> getTotalAdminsCount() {
        try {
            long totalAdmins = userRepository.countByRole(Role.ADMIN);
            return ResponseEntity.ok(totalAdmins);
        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }

    @GetMapping("/directors/total-count")
    public ResponseEntity<Long> getTotalDirectorsCount() {
        try {
            long totalDirectors = userRepository.countByRole(Role.DIRECTOR);
            return ResponseEntity.ok(totalDirectors);
        } catch (Exception e) {
            return ResponseEntity.ok(0L);
        }
    }

    // ✅ Get user role by email (for deactivated user modal)
    @GetMapping("/role-by-email")
    public ResponseEntity<Map<String, String>> getUserRoleByEmail(@RequestParam String email) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                Map<String, String> response = new HashMap<>();
                response.put("role", userOpt.get().getRole().name());
                response.put("email", email);
                response.put("status", userOpt.get().getStatus().toString());
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "Failed to fetch user role"));
        }
    }
}
