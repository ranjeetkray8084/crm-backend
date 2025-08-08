package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.Note;
import com.example.real_estate_crm.model.Note.Visibility;
import com.example.real_estate_crm.model.Note.Status;
import com.example.real_estate_crm.model.Note.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByCompany_Id(Long companyId);

    Optional<Note> findByIdAndCompany_Id(Long noteId, Long companyId);

    @Modifying
    @Query("DELETE FROM Note n WHERE n.id = :noteId AND n.company.id = :companyId")
    void deleteByIdAndCompany_Id(@Param("noteId") Long noteId, @Param("companyId") Long companyId);

    @Query("""
        SELECT DISTINCT n FROM Note n
        WHERE n.company.id = :companyId
        AND (
            (n.visibility = 'ALL_USERS' AND n.userId IN :allowedUserIds)
            OR (n.visibility = 'SPECIFIC_USERS' AND :userId IN elements(n.visibleUserIds))
            OR (n.visibility = 'ONLY_ME' AND n.userId = :userId)
            OR (n.visibility = 'ME_AND_ADMIN' AND (n.userId = :userId OR :isAdmin = true))
            OR (n.visibility = 'ME_AND_DIRECTOR' AND (n.userId = :userId OR :isDirector = true))
            OR (n.visibility = 'ALL_ADMIN' AND :isAdmin = true)
            OR (n.visibility = 'SPECIFIC_ADMIN' AND :isAdmin = true AND :userId IN elements(n.visibleUserIds))
        )
    """)
    List<Note> findNotesVisibleToUserAndCompanyWithFilteredAllUsers(
        @Param("userId") Long userId,
        @Param("companyId") Long companyId,
        @Param("isAdmin") boolean isAdmin,
        @Param("isDirector") boolean isDirector,
        @Param("allowedUserIds") List<Long> allowedUserIds
    );

    List<Note> findByVisibilityAndCompany_Id(Visibility visibility, Long companyId);

    List<Note> findByCompany_IdAndVisibilityIn(Long companyId, List<Visibility> visibilities);

    List<Note> findByVisibility(Visibility visibility);

    List<Note> findByVisibleUserIdsContaining(Long userId);

    List<Note> findByUserIdAndCompany_Id(Long userId, Long companyId);

    List<Note> findByUserId(Long userId);

    List<Note> findByStatus(Status status);

    List<Note> findByCompany_IdAndStatus(Long companyId, Status status);

    List<Note> findByUserIdAndStatus(Long userId, Status status);

    List<Note> findByUserIdAndCompany_IdAndStatus(Long userId, Long companyId, Status status);

    List<Note> findByPriority(Priority priority);

    List<Note> findByCompany_IdAndPriority(Long companyId, Priority priority);

    List<Note> findByUserIdAndCompany_IdAndPriority(Long userId, Long companyId, Priority priority);

    List<Note> findByUserIdAndPriority(Long userId, Priority priority);

    List<Note> findByCompany_IdAndPriorityAndStatus(Long companyId, Priority priority, Status status);

    List<Note> findByUserIdAndCompany_IdAndPriorityAndStatus(Long userId, Long companyId, Priority priority, Status status);

    List<Note> findByCompany_IdOrderByPriorityAsc(Long companyId);

    List<Note> findByCompany_IdOrderByPriorityDesc(Long companyId);

    List<Note> findAllByCompany_IdAndVisibilityInAndUserIdIn(
        Long companyId,
        List<Note.Visibility> visibilities,
        List<Long> userIds
    );

    List<Note> findNotesByCompanyIdAndVisibilityInAndUserId(Long companyId, List<Note.Visibility> visibilities, Long userId);

    // ✅ ✅ ✅ FIXED METHOD (previous one was incorrect)
    @Query("""
        SELECT n FROM Note n 
        WHERE n.company.id = :companyId 
        AND n.visibility IN :visibilities 
        AND n.userId = :userId
    """)
    List<Note> findByCompanyIdAndVisibilityInAndUserIdCustom(
        @Param("companyId") Long companyId,
        @Param("visibilities") List<Note.Visibility> visibilities,
        @Param("userId") Long userId
    );

    // ✅ For director role (used in director-visible API)
    @Query("""
    	    SELECT DISTINCT n FROM Note n
    	    WHERE n.company.id = :companyId
    	    AND (
    	        (n.visibility = 'ALL_USERS' AND n.userId = :directorId)
    	        OR (n.visibility = 'ME_AND_DIRECTOR' AND (n.userId = :directorId OR :isDirector = true))
    	        OR (n.visibility = 'SPECIFIC_USERS' AND (n.userId = :directorId OR :directorId IN elements(n.visibleUserIds)))
    	        OR (n.visibility = 'ALL_ADMIN')
    	        OR (n.visibility = 'SPECIFIC_ADMIN' AND :directorId IN elements(n.visibleUserIds))
    	        OR (n.visibility = 'ONLY_ME' AND n.userId = :directorId)
    	    )
    	""")

    List<Note> findNotesVisibleToDirector(
        @Param("companyId") Long companyId,
        @Param("directorId") Long directorId,
        @Param("isDirector") boolean isDirector
    );
    
    List<Note> findAllByCompany_IdAndVisibility(Long companyId, Visibility visibility);
    
    
    List<Note> findAllByCompany_IdAndVisibilityAndVisibleUserIdsContaining(Long companyId, Visibility visibility, Long adminId);

}
