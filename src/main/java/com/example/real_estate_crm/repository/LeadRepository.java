package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Lead;
import com.example.real_estate_crm.model.Lead.Action;
import com.example.real_estate_crm.model.Lead.LeadStatus;
import com.example.real_estate_crm.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    List<Lead> findByStatus(LeadStatus status);
    List<Lead> findByAssignedTo(User assignedTo);
    List<Lead> findBySource(Lead.Source source);
    List<Lead> findByAction(Action action);
    List<Lead> findByNameContainingIgnoreCase(String name);
    List<Lead> findByCreatedBy(User createdBy);
    List<Lead> findByCompany(Company company);
    Optional<Lead> findByLeadIdAndCompany(Long leadId, Company company);
    List<Lead> findByStatusAndCompany(LeadStatus status, Company company);
    List<Lead> findBySourceAndCompany(Lead.Source source, Company company);

    @Query("SELECT l FROM Lead l WHERE l.createdBy.userId = :userId AND l.company = :company")
    List<Lead> findByCreatorIdAndCompany(@Param("userId") Long userId, @Param("company") Company company);

    List<Lead> findByAssignedToUserIdAndCompany(Long userId, Company company);
    List<Lead> findByNameContainingIgnoreCaseAndCompany(String name, Company company);
    Page<Lead> findByCompany_Id(Long companyId, Pageable pageable);

    // ✅ Fix starts here
    @Query("SELECT COUNT(l) FROM Lead l")
    long countAll();

    @Query("SELECT COUNT(l) FROM Lead l WHERE l.assignedTo.id = :userId")
    long countByAssignedTo(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(l) FROM Lead l WHERE l.status = :status")
    long countByStatus(@Param("status") Lead.LeadStatus status);
	Page<Lead> findByCompanyIdAndAssignedTo_UserId(Long companyId, Long userId, Pageable pageable);
	
	Page<Lead> findByCompany_IdAndStatusNot(Long companyId, Lead.LeadStatus status, Pageable pageable);

	Page<Lead> findByCompany_IdAndStatusNotAndCreatedBy_UserIdOrAssignedTo_UserId(
		    Long companyId,
		    Lead.LeadStatus status,
		    Long createdByUserId,
		    Long assignedToUserId,
		    Pageable pageable
		);


	
	Page<Lead> findByCompanyIdAndCreatedBy_UserId(Long companyId, Long userId, Pageable pageable);

	@Query("SELECT l FROM Lead l WHERE l.company.id = :companyId AND (l.createdBy.userId = :userId OR l.assignedTo.userId = :userId)")
	Page<Lead> findByCompanyIdAndCreatedByOrAssignedTo(
	    @Param("companyId") Long companyId,
	    @Param("userId") Long userId,
	    Pageable pageable
	);
	long countByCompany_Id(Long companyId);
	long countByCompany_IdAndStatus(Long companyId, LeadStatus closed);
	
	// ✅ New method to count leads with multiple statuses (CLOSED and DROPED)
	@Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND l.status IN (:statuses)")
	long countByCompany_IdAndStatusIn(@Param("companyId") Long companyId, @Param("statuses") List<LeadStatus> statuses);
	
	long countByCompanyIdAndAssignedToUserId(Long companyId, Long userId);
	List<Lead> findByCompanyIdAndCreatedByUserId(Long companyId, Long userId);
	List<Lead> findByCompanyIdAndAssignedToUserId(Long companyId, Long userId);
	 
	@Query(value = """
		    SELECT l.lead_id, l.action, l.budget, l.created_at, l.created_by,
		           l.email, l.name, l.phone, l.reference_name, l.requirement,
		           l.source, l.status, l.updated_at, l.version,
		           l.assigned_to, l.company_id, l.location
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (
		           :search IS NULL OR 
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:search), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :search, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:search), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:createdBy IS NULL OR l.created_by = :createdBy)
		      AND (:source IS NULL OR l.source = :source)         -- ✅ NEW
		      AND (:action IS NULL OR l.action = :action)         -- ✅ NEW
		    ORDER BY l.created_at DESC
		    """,
		    countQuery = """
		    SELECT COUNT(*)
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (
		           :search IS NULL OR 
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:search), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :search, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:search), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:createdBy IS NULL OR l.created_by = :createdBy)
		      AND (:source IS NULL OR l.source = :source)         -- ✅ NEW
		      AND (:action IS NULL OR l.action = :action)         -- ✅ NEW
		    """,
		    nativeQuery = true
		)
		Page<Lead> searchLeads(
		    @Param("companyId") Long companyId,
		    @Param("search") String search,
		    @Param("status") String status,
		    @Param("minBudget") BigDecimal minBudget,
		    @Param("maxBudget") BigDecimal maxBudget,
		    @Param("createdBy") Long createdBy,
		    @Param("source") String source,           // ✅ NEW
		    @Param("action") String action,           // ✅ NEW
		    Pageable pageable
		);

	
	@Query(value = """
		    SELECT l.lead_id, l.action, l.budget, l.created_at, l.created_by,
		           l.email, l.name, l.phone, l.reference_name, l.requirement,
		           l.source, l.status, l.updated_at, l.version,
		           l.assigned_to, l.company_id, l.location
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (:userId IS NULL OR l.created_by = :userId OR l.assigned_to = :userId)
		      AND (
		           :search IS NULL OR 
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:search), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :search, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:search), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:source IS NULL OR l.source = :source)
		      AND (:action IS NULL OR l.action = :action)
		    ORDER BY l.created_at DESC
		    """,
		    countQuery = """
		    SELECT COUNT(*)
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (:userId IS NULL OR l.created_by = :userId OR l.assigned_to = :userId)
		      AND (
		           :search IS NULL OR 
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:search), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :search, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:search), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:source IS NULL OR l.source = :source)
		      AND (:action IS NULL OR l.action = :action)
		    """,
		    nativeQuery = true
		)
		Page<Lead> searchLeadsCreatedOrAssigned(
		    @Param("companyId") Long companyId,
		    @Param("userId") Long userId,
		    @Param("search") String search,
		    @Param("status") String status,
		    @Param("minBudget") BigDecimal minBudget,
		    @Param("maxBudget") BigDecimal maxBudget,
		    @Param("source") String source,
		    @Param("action") String action,
		    Pageable pageable
		);

	
	@Query(value = """
		    SELECT l.lead_id, l.action, l.budget, l.created_at, l.created_by,
		           l.email, l.name, l.phone, l.reference_name, l.requirement,
		           l.source, l.status, l.updated_at, l.version,
		           l.assigned_to, l.company_id, l.location
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (
		          cby.user_id = :adminId
		          OR cby.admin_id = :adminId
		          OR ato.admin_id = :adminId
		      )
		      AND (:createdBy IS NULL OR l.created_by = :createdBy)
		      AND (
		          :search IS NULL OR 
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:search), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :search, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:search), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:source IS NULL OR l.source = :source)
		      AND (:action IS NULL OR l.action = :action)
		    ORDER BY l.created_at DESC
		    """,
		    countQuery = """
		    SELECT COUNT(*)
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (
		          cby.user_id = :adminId
		          OR cby.admin_id = :adminId
		          OR ato.admin_id = :adminId
		      )
		      AND (:createdBy IS NULL OR l.created_by = :createdBy)
		      AND (
		          :search IS NULL OR 
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:search), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :search, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:search), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:search), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:source IS NULL OR l.source = :source)
		      AND (:action IS NULL OR l.action = :action)
		    """,
		    nativeQuery = true
		)
		Page<Lead> searchLeadsVisibleToAdmin(
		    @Param("companyId") Long companyId,
		    @Param("adminId") Long adminId,
		    @Param("createdBy") Long createdBy,  // ✅ New param added
		    @Param("search") String search,
		    @Param("status") String status,
		    @Param("minBudget") BigDecimal minBudget,
		    @Param("maxBudget") BigDecimal maxBudget,
		    @Param("source") String source,
		    @Param("action") String action,
		    Pageable pageable
		);

	@Query(value = """
		    SELECT COUNT(DISTINCT l.lead_id)
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND l.status NOT IN ('DROPPED', 'CLOSED')
		      AND (
		        l.created_by = :adminId
		        OR cby.admin_id = :adminId
		        OR ato.admin_id = :adminId
		      )
		""", nativeQuery = true)
		long countLeadsVisibleToAdmin(@Param("companyId") Long companyId, @Param("adminId") Long adminId);

	
	
	
	
	@Query(value = """
		    SELECT * FROM leads l
		    WHERE l.company_id = :companyId
		      AND (
		          l.created_by = :adminId
		          OR l.assigned_to = :adminId
		          OR l.created_by IN (SELECT user_id FROM users WHERE admin_id = :adminId)
		          OR l.assigned_to IN (SELECT user_id FROM users WHERE admin_id = :adminId)
		      )
		      AND l.status != 'DROPED'
		    ORDER BY l.created_at DESC
		    """,
		    countQuery = """
		    SELECT COUNT(*) FROM leads l
		    WHERE l.company_id = :companyId
		      AND (
		          l.created_by = :adminId
		          OR l.assigned_to = :adminId
		          OR l.created_by IN (SELECT user_id FROM users WHERE admin_id = :adminId)
		          OR l.assigned_to IN (SELECT user_id FROM users WHERE admin_id = :adminId)
		      )
		      AND l.status != 'DROPED'
		    """,
		    nativeQuery = true
		)
		Page<Lead> getLeadsVisibleToAdmin(
		    @Param("companyId") Long companyId,
		    @Param("adminId") Long adminId,
		    Pageable pageable
		);
	
	@Query(value = """
		    SELECT COUNT(DISTINCT l.lead_id)
		    FROM leads l
		    WHERE l.status IN ('CLOSED')
		      AND l.company_id = :companyId
		      AND (
		        l.created_by = :adminId
		        OR l.created_by IN (
		            SELECT user_id FROM users WHERE admin_id = :adminId
		        )
		        OR l.assigned_to IN (
		            SELECT user_id FROM users WHERE admin_id = :adminId
		        )
		      )
		""", nativeQuery = true)
		Long countClosedLeadsForAdmin(@Param("companyId") Long companyId, @Param("adminId") Long adminId);


	
	// ✅ Count total leads visible to admin (created by admin or their users OR assigned to them)
	@Query(value = """
	    SELECT COUNT(DISTINCT l.lead_id)
	    FROM leads l
	    WHERE l.company_id = :companyId
	      AND (
	        l.created_by = :adminId
	        OR l.created_by IN (SELECT user_id FROM users WHERE admin_id = :adminId)
	        OR l.assigned_to IN (SELECT user_id FROM users WHERE admin_id = :adminId)
	      )
	    """, nativeQuery = true)
	Long countTotalLeadsForAdmin(@Param("companyId") Long companyId, @Param("adminId") Long adminId);

	// ✅ Count leads with status = 'NEW' for admin and their team
	@Query(value = """
	    SELECT COUNT(DISTINCT l.lead_id)
	    FROM leads l
	    WHERE l.company_id = :companyId
	      AND l.status = 'NEW'
	      AND (
	        l.created_by = :adminId
	        OR l.created_by IN (SELECT user_id FROM users WHERE admin_id = :adminId)
	        OR l.assigned_to IN (SELECT user_id FROM users WHERE admin_id = :adminId)
	      )
	    """, nativeQuery = true)
	Long countNewLeadsForAdmin(@Param("companyId") Long companyId, @Param("adminId") Long adminId);

	// ✅ Count leads with status = 'CONTACTED' for admin and their team
	@Query(value = """
	    SELECT COUNT(DISTINCT l.lead_id)
	    FROM leads l
	    WHERE l.company_id = :companyId
	      AND l.status = 'CONTACTED'
	      AND (
	        l.created_by = :adminId
	        OR l.created_by IN (SELECT user_id FROM users WHERE admin_id = :adminId)
	        OR l.assigned_to IN (SELECT user_id FROM users WHERE admin_id = :adminId)
	      )
	    """, nativeQuery = true)
	Long countContactedLeadsForAdmin(@Param("companyId") Long companyId, @Param("adminId") Long adminId);

	
	// ✅ Total leads created by user
	@Query(value = """
	    SELECT COUNT(*)
	    FROM leads l
	    WHERE l.company_id = :companyId
	      AND l.created_by = :userId
	    """, nativeQuery = true)
	Long countTotalLeadsForUser(@Param("companyId") Long companyId, @Param("userId") Long userId);

	// ✅ NEW leads created by user
	@Query(value = """
	    SELECT COUNT(*)
	    FROM leads l
	    WHERE l.company_id = :companyId
	      AND l.created_by = :userId
	      AND l.status = 'NEW'
	    """, nativeQuery = true)
	Long countNewLeadsForUser(@Param("companyId") Long companyId, @Param("userId") Long userId);

	// ✅ CONTACTED leads created by user
	@Query(value = """
	    SELECT COUNT(*)
	    FROM leads l
	    WHERE l.company_id = :companyId
	      AND l.created_by = :userId
	      AND l.status = 'CONTACTED'
	    """, nativeQuery = true)
	Long countContactedLeadsForUser(@Param("companyId") Long companyId, @Param("userId") Long userId);
	// ✅ Corrected method signatures using userId in related entities
	long countByCompany_IdAndStatusAndCreatedBy_UserIdInOrAssignedTo_UserIdIn(
	        Long companyId,
	        LeadStatus status,
	        List<Long> createdByUserIds,
	        List<Long> assignedToUserIds
	);

	long countByCompany_IdAndStatusAndCreatedBy_UserId(
	        Long companyId,
	        LeadStatus status,
	        Long createdByUserId
	);

	long countByCompany_IdAndCreatedBy_UserId(
	        Long companyId,
	        Long createdByUserId
	);

	long countByCompany_IdAndCreatedBy_UserIdInOrCompany_IdAndAssignedTo_UserIdIn(
	        Long companyId1,
	        List<Long> createdByUserIds,
	        Long companyId2,
	        List<Long> assignedToUserIds
	);

	long countByCompany_IdAndCreatedBy_UserIdInOrAssignedTo_UserIdIn(
	        Long companyId,
	        List<Long> createdByUserIds,
	        List<Long> assignedToUserIds
	);

	// Methods to exclude DROPPED leads
	long countByCompany_IdAndStatusNot(Long companyId, LeadStatus status);
	
	// Custom queries for ADMIN role
	@Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND l.status != 'DROPED' AND (l.createdBy.userId IN :userIds OR l.assignedTo.userId IN :userIds)")
	long countActiveLeadsByUserIds(@Param("companyId") Long companyId, @Param("userIds") List<Long> userIds);
	
	@Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND l.status = :status AND (l.createdBy.userId IN :userIds OR l.assignedTo.userId IN :userIds)")
	long countActiveLeadsByStatusAndUserIds(@Param("companyId") Long companyId, @Param("status") LeadStatus status, @Param("userIds") List<Long> userIds);
	
	// Custom queries for USER role
	@Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND l.status != 'DROPED' AND l.createdBy.userId = :userId")
	long countActiveLeadsByCreatedBy(@Param("companyId") Long companyId, @Param("userId") Long userId);
	
	@Query("SELECT COUNT(l) FROM Lead l WHERE l.company.id = :companyId AND l.status = :status AND l.createdBy.userId = :userId")
	long countActiveLeadsByStatusAndCreatedBy(@Param("companyId") Long companyId, @Param("status") LeadStatus status, @Param("userId") Long userId);
	
	// Custom queries for USER role - count both created and assigned leads
	@Query("SELECT COUNT(DISTINCT l) FROM Lead l WHERE l.company.id = :companyId AND l.status != 'DROPED' AND (l.createdBy.userId = :userId OR l.assignedTo.userId = :userId)")
	long countActiveLeadsByCreatedByOrAssignedTo(@Param("companyId") Long companyId, @Param("userId") Long userId);
	
	@Query("SELECT COUNT(DISTINCT l) FROM Lead l WHERE l.company.id = :companyId AND l.status = :status AND (l.createdBy.userId = :userId OR l.assignedTo.userId = :userId)")
	long countActiveLeadsByStatusAndCreatedByOrAssignedTo(@Param("companyId") Long companyId, @Param("status") LeadStatus status, @Param("userId") Long userId);


	
	// ✅ New method for multiple keywords search with AND logic
	@Query(value = """
		    SELECT l.lead_id, l.action, l.budget, l.created_at, l.created_by,
		           l.email, l.name, l.phone, l.reference_name, l.requirement,
		           l.source, l.status, l.updated_at, l.version,
		           l.assigned_to, l.company_id, l.location
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :keyword1, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		      )
		      AND (
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :keyword2, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:createdBy IS NULL OR l.created_by = :createdBy)
		      AND (:source IS NULL OR l.source = :source)
		      AND (:action IS NULL OR l.action = :action)
		    ORDER BY l.created_at DESC
		    """,
		    countQuery = """
		    SELECT COUNT(*)
		    FROM leads l
		    LEFT JOIN users cby ON l.created_by = cby.user_id
		    LEFT JOIN users ato ON l.assigned_to = ato.user_id
		    WHERE l.company_id = :companyId
		      AND (
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :keyword1, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:keyword1), '%')
		      )
		      AND (
		           LOWER(l.name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.phone) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.email) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.location) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.reference_name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.requirement) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.source) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.status) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(l.action) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR CAST(l.budget AS CHAR) LIKE CONCAT('%', :keyword2, '%')
		           OR LOWER(cby.name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		           OR LOWER(ato.name) LIKE CONCAT('%', LOWER(:keyword2), '%')
		      )
		      AND (:status IS NULL OR l.status = :status)
		      AND (:minBudget IS NULL OR l.budget >= :minBudget)
		      AND (:maxBudget IS NULL OR l.budget <= :maxBudget)
		      AND (:createdBy IS NULL OR l.created_by = :createdBy)
		      AND (:source IS NULL OR l.source = :source)
		      AND (:action IS NULL OR l.action = :action)
		    """,
		    nativeQuery = true
		)
		Page<Lead> searchLeadsWithTwoKeywords(
		    @Param("companyId") Long companyId,
		    @Param("keyword1") String keyword1,
		    @Param("keyword2") String keyword2,
		    @Param("status") String status,
		    @Param("minBudget") BigDecimal minBudget,
		    @Param("maxBudget") BigDecimal maxBudget,
		    @Param("createdBy") Long createdBy,
		    @Param("source") String source,
		    @Param("action") String action,
		    Pageable pageable
		);

	
}
