package com.example.real_estate_crm.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.real_estate_crm.model.Note;

import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // Find all notes created by a specific user
    List<Note> findByUserId(Long userId);

    // Find all notes visible to a specific user (for SPECIFIC_USERS visibility)
    List<Note> findByVisibleUserIdsContaining(Long userId);

    // (Optional) Find notes by visibility type
    List<Note> findByVisibility(Note.Visibility visibility);
    List<Note> findByVisibilityIn(List<Note.Visibility> visibilities);

    // Custom query to find notes visible to a specific user based on visibility logic
    @Query("SELECT DISTINCT n FROM Note n " +
            "LEFT JOIN n.visibleUserIds vu " +
            "WHERE n.visibility = 'ALL_USERS' " +
            "   OR (n.visibility = 'SPECIFIC_USERS' AND :userId IN elements(n.visibleUserIds)) " +
            "   OR (n.userId = :userId) " +
            "   OR (n.visibility = 'ME_AND_ADMIN' AND n.userId = :userId)")
     List<Note> findNotesVisibleToUser(@Param("userId") Long userId);
}
