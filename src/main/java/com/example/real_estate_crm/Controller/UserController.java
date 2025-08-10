package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.dto.UserDTO;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.UserAvatar;
import com.example.real_estate_crm.model.User.Role;
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
        return userRepository.findByCompanyIdAndRole(companyId, Role.ADMIN).stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/role/{companyId}/{role}")
    public List<UserDTO> getUsersByRole(@PathVariable Long companyId, @PathVariable Role role) {
        return userRepository.findByCompanyIdAndRole(companyId, role).stream().map(UserDTO::new).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<User> user = userDao.getUserById(id);
        return user.map(value -> ResponseEntity.ok(new UserDTO(value))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            // Step 1: Null check safely
            if (user.getCompany() == null) {
                System.out.println("❌ Company object is null");
                return ResponseEntity.badRequest().body("Company is required");
            }

            if (user.getCompany().getId() == null) {
                System.out.println("❌ Company ID is null");
                return ResponseEntity.badRequest().body("Company ID is required");
            }

            // Step 2: Check if company exists
            Optional<Company> optionalCompany = Optional.ofNullable(userDao.findCompanyById(user.getCompany().getId()));
            if (optionalCompany.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid company ID");
            }

            Company company = optionalCompany.get();

            // Step 3: Limit checks
            long existingCount = userRepository.countByCompanyAndRole(company, user.getRole());

            if (user.getRole() == Role.USER && company.getMaxUsers() != null && existingCount >= company.getMaxUsers()) {
                return ResponseEntity.badRequest().body("❌ User creation failed: Max user limit reached for this company.");
            }

            if (user.getRole() == Role.ADMIN && company.getMaxAdmins() != null && existingCount >= company.getMaxAdmins()) {
                return ResponseEntity.badRequest().body("❌ Admin creation failed: Max admin limit reached for this company.");
            }

            // Step 4: Save user
            user.setCompany(company); // Just to be sure
            User savedUser = userDao.save(user);

            return ResponseEntity.status(HttpStatus.CREATED).body(new UserDTO(savedUser));

        } catch (Exception e) {
            e.printStackTrace(); // For debug in logs
            return ResponseEntity.badRequest().body("❌ Error creating user: " + e.getMessage());
        }
    }

    @PutMapping("/update-profile/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User user) {
        Optional<User> optionalUser = userDao.findById(id);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }

        User existingUser = optionalUser.get();
        existingUser.setName(user.getName());
        existingUser.setEmail(user.getEmail());
        existingUser.setPhone(user.getPhone());
        existingUser.setRole(user.getRole());
        userDao.updateUser(existingUser);
        return ResponseEntity.ok(new UserDTO(existingUser));
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
        List<UserDTO> users = userRepository.findByCompany_Id(companyId).stream().map(UserDTO::new).collect(Collectors.toList());
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
        List<UserDTO> users = userDao.findUsersByAdminId(adminId).stream().map(UserDTO::new).collect(Collectors.toList());
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
        List<UserDTO> admins = userDao.findAdminsByCompanyId(companyId).stream().map(UserDTO::new).collect(Collectors.toList());
        return ResponseEntity.ok(admins);
    }

    @GetMapping("/company/{companyId}/users")
    public ResponseEntity<List<UserDTO>> getUsersByCompany(@PathVariable Long companyId) {
        List<UserDTO> users = userDao.findUsersByCompanyId(companyId).stream().map(UserDTO::new).collect(Collectors.toList());
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
            return ResponseEntity.ok("Admin assigned successfully");
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

        return ResponseEntity.ok("Admin unassigned successfully.");
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
            @PathVariable Long userId
    ) {
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
}
