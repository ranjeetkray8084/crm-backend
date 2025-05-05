package com.example.real_estate_crm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.real_estate_crm.model.User.Role; // ✅ Correct import
import com.example.real_estate_crm.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email); // Already present

    User findByEmailAndPassword(String email, String password); // Add this

    public List<User> findByRole(Role role); // Use Role enum, not String
    
    


}
