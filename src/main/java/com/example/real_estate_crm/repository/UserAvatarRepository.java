package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.UserAvatar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAvatarRepository extends JpaRepository<UserAvatar, Long> {
    
    // Get all avatars by userId
    List<UserAvatar> findByUserId(Long userId);
    
    // ✅ Get the latest uploaded avatar for a user
    UserAvatar findFirstByUserIdOrderByIdDesc(Long userId);
}
