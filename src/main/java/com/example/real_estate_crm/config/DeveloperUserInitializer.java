package com.example.real_estate_crm.config;

import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.User.Role;
import com.example.real_estate_crm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class DeveloperUserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createDeveloperUser("ranjeetkumarroya23@gmail.com", "Dev@1234", "Ranjeet Kumar Ray", "8084167515");
        createDeveloperUser("anshurajsinha9@gmail.com", "AdeVop@2001", "Anshu Raj", "9570361707");
    }

    private void createDeveloperUser(String email, String password, String name, String phone) {
        // ✅ Check if developer user already exists by email
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isEmpty()) {
            // Create developer user only if not present
            User developer = new User();
            developer.setName(name);
            developer.setEmail(email);
            developer.setPassword(passwordEncoder.encode(password));
            developer.setPhone(phone);
            developer.setRole(Role.DEVELOPER);
            developer.setStatus(true);
            developer.setCreatedAt(LocalDateTime.now());

            userRepository.save(developer);
        }
    }
}
