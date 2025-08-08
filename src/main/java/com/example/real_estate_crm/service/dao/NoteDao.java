package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Note;
import com.example.real_estate_crm.model.Note.Visibility;
import com.example.real_estate_crm.model.Note.Status;
import com.example.real_estate_crm.model.Note.Priority;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoteDao {

    // ======================
    // Basic CRUD Operations
    // ======================

    Note saveNote(Note note);

    Note updateNote(Note note);

    Optional<Note> findNoteById(Long noteId);

    void deleteNoteById(Long noteId);

    List<Note> findAllNotes();

    // ======================
    // Company-Scoped Methods (Preferred for Multi-Tenancy)
    // ======================

    List<Note> findAllNotesByCompany(Long companyId);

    Optional<Note> findNoteByIdAndCompany(Long noteId, Long companyId);

    void deleteNoteByIdAndCompany(Long noteId, Long companyId);

    List<Note> findNotesByUserIdAndCompanyId(Long userId, Long companyId);

    List<Note> findNotesByVisibilityAndCompanyId(Visibility visibility, Long companyId);

    List<Note> findNotesByVisibilityInCompany(Long companyId, List<Visibility> visibilities);

    // ✅ UPDATED to support admin + director role-based visibility
    List<Note> findNotesVisibleToUserAndCompany(Long userId, Long companyId, boolean isAdmin, boolean isDirector);

    // ======================
    // Legacy / Non-Company Scoped Methods
    // ======================

    List<Note> findNotesByUserId(Long userId);

    List<Note> findNotesByVisibility(Visibility visibility);

    List<Note> findNotesVisibleToUser(Long userId);

    List<Note> findNotesVisibleToUser(Long userId, Long companyId);

    List<Note> findNotesByVisibility(Long companyId, List<Visibility> visibilities);

    // ======================
    // Status-Based Methods
    // ======================

    List<Note> findNotesByStatus(Status status);

    List<Note> findNotesByCompanyIdAndStatus(Long companyId, Status status);

    List<Note> findNotesByUserIdAndStatus(Long userId, Status status);

    List<Note> findNotesByUserIdAndCompanyIdAndStatus(Long userId, Long companyId, Status status);

    // ======================
    // ✅ Priority-Based Methods
    // ======================

    List<Note> findNotesByPriority(Priority priority);

    List<Note> findNotesByCompanyIdAndPriority(Long companyId, Priority priority);

    List<Note> findNotesByUserIdAndPriority(Long userId, Priority priority);

    List<Note> findNotesByUserIdAndCompanyIdAndPriority(Long userId, Long companyId, Priority priority);

    List<Note> findNotesByCompanyIdAndPriorityAndStatus(Long companyId, Priority priority, Status status);

    List<Note> findNotesByUserIdAndCompanyIdAndPriorityAndStatus(Long userId, Long companyId, Priority priority, Status status);

    List<Note> findNotesSortedByPriorityAsc(Long companyId);

    List<Note> findNotesSortedByPriorityDesc(Long companyId);

	List<Note> findNotesVisibleToUserAndCompany(Long userId, Long companyId, boolean isAdmin);
	
	List<Note> findNotesByCompanyIdAndVisibilityAndUserId(Long companyId, List<Note.Visibility> visibilities, Long userId);

	
	  
   
}
