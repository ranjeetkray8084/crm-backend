package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.PushToken;
import com.example.real_estate_crm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PushTokenRepository extends JpaRepository<PushToken, Long> {

    Optional<PushToken> findByUserAndDeviceType(User user, String deviceType);
    
    List<PushToken> findByUserAndIsActiveTrue(User user);
    
    /**
     * 🔒 SECURITY: Find active push tokens by user ID (for logout)
     */
    List<PushToken> findByUserUserIdAndIsActiveTrue(Long userId);
    
    /**
     * 🔒 SECURITY: Company-wide push token queries are NOT allowed
     * This query has been removed to prevent cross-company notifications
     * Use findByUserAndIsActiveTrue() for user-specific tokens instead
     */
    // @Query("SELECT pt FROM PushToken pt WHERE pt.user.company.id = :companyId AND pt.isActive = true")
    // List<PushToken> findByUserCompanyCompanyIdAndIsActiveTrue(@Param("companyId") Long companyId);
    
    @Query("SELECT pt FROM PushToken pt WHERE pt.user.userId IN :userIds AND pt.isActive = true")
    List<PushToken> findByUserIdInAndIsActiveTrue(@Param("userIds") List<Long> userIds);
    
    List<PushToken> findByUser(User user);
    
    void deleteByUser(User user);
}
