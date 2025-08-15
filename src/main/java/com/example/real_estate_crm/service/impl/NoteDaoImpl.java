package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Note;
import com.example.real_estate_crm.model.Note.Priority;
import com.example.real_estate_crm.model.Note.Status;
import com.example.real_estate_crm.model.Note.Visibility;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.NoteRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.dao.NoteDao;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Transactional
public class NoteDaoImpl implements NoteDao {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @Autowired
    public NoteDaoImpl(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    // ============= CRUD =============

    @Override
    public Note saveNote(Note note) {
        try {
            // Ensure visibleUserIds is not null for specific visibility types
            if ((note.getVisibility() == Visibility.SPECIFIC_USERS || 
                 note.getVisibility() == Visibility.SPECIFIC_ADMIN) && 
                note.getVisibleUserIds() == null) {
                note.setVisibleUserIds(new ArrayList<>());
            }
            
            return noteRepository.save(note);
        } catch (Exception e) {
            System.err.println("Error saving note: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public Note updateNote(Note note) {
        return noteRepository.save(note);
    }

    @Override
    public Optional<Note> findNoteById(Long noteId) {
        return noteRepository.findById(noteId);
    }

    @Override
    public void deleteNoteById(Long noteId) {
        noteRepository.deleteById(noteId);
    }

    @Override
    public List<Note> findAllNotes() {
        return noteRepository.findAll();
    }

    // ============= Company Scoped =============

    @Override
    public List<Note> findAllNotesByCompany(Long companyId) {
        return noteRepository.findByCompany_Id(companyId);
    }

    @Override
    public Optional<Note> findNoteByIdAndCompany(Long noteId, Long companyId) {
        return noteRepository.findByIdAndCompany_Id(noteId, companyId);
    }

    @Override
    public void deleteNoteByIdAndCompany(Long noteId, Long companyId) {
        noteRepository.deleteByIdAndCompany_Id(noteId, companyId);
    }

    @Override
    public List<Note> findNotesByUserIdAndCompanyId(Long userId, Long companyId) {
        return noteRepository.findByUserIdAndCompany_Id(userId, companyId);
    }

    @Override
    public List<Note> findNotesByVisibilityAndCompanyId(Visibility visibility, Long companyId) {
        return noteRepository.findByVisibilityAndCompany_Id(visibility, companyId);
    }

    @Override
    public List<Note> findNotesByVisibilityInCompany(Long companyId, List<Visibility> visibilities) {
        return noteRepository.findByCompany_IdAndVisibilityIn(companyId, visibilities);
    }

    // ============= UPDATED VISIBILITY LOGIC =============

    @Override
    public List<Note> findNotesVisibleToUserAndCompany(Long userId, Long companyId, boolean isAdmin) {
        return findNotesVisibleToUserAndCompany(userId, companyId, isAdmin, false);
    }

    public List<Note> findNotesVisibleToUserAndCompany(Long userId, Long companyId, boolean isAdmin, boolean isDirector) {
        List<Long> allowedUserIds = getAllowedUserIds(userId);
        return noteRepository.findNotesVisibleToUserAndCompanyWithFilteredAllUsers(
                userId, companyId, isAdmin, isDirector, allowedUserIds
        );
    }

    private List<Long> getAllowedUserIds(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.getAdmin() != null) {
                return List.of(userId, user.getAdmin().getUserId());
            }
        }
        return List.of(userId);
    }

    // ============= Legacy =============

    @Override
    public List<Note> findNotesByUserId(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    @Override
    public List<Note> findNotesByVisibility(Visibility visibility) {
        return noteRepository.findByVisibility(visibility);
    }

    @Override
    public List<Note> findNotesVisibleToUser(Long userId) {
        return noteRepository.findByVisibleUserIdsContaining(userId);
    }

    @Override
    public List<Note> findNotesVisibleToUser(Long userId, Long companyId) {
        return findNotesVisibleToUserAndCompany(userId, companyId, false, false);
    }

    @Override
    public List<Note> findNotesByVisibility(Long companyId, List<Visibility> visibilities) {
        return noteRepository.findByCompany_IdAndVisibilityIn(companyId, visibilities);
    }

    // ============= Status =============

    @Override
    public List<Note> findNotesByStatus(Status status) {
        return noteRepository.findByStatus(status);
    }

    @Override
    public List<Note> findNotesByCompanyIdAndStatus(Long companyId, Status status) {
        return noteRepository.findByCompany_IdAndStatus(companyId, status);
    }

    @Override
    public List<Note> findNotesByUserIdAndStatus(Long userId, Status status) {
        return noteRepository.findByUserIdAndStatus(userId, status);
    }

    @Override
    public List<Note> findNotesByUserIdAndCompanyIdAndStatus(Long userId, Long companyId, Status status) {
        return noteRepository.findByUserIdAndCompany_IdAndStatus(userId, companyId, status);
    }

    // ============= Priority =============

    @Override
    public List<Note> findNotesByPriority(Priority priority) {
        return noteRepository.findByPriority(priority);
    }

    @Override
    public List<Note> findNotesByCompanyIdAndPriority(Long companyId, Priority priority) {
        return noteRepository.findByCompany_IdAndPriority(companyId, priority);
    }

    @Override
    public List<Note> findNotesByUserIdAndPriority(Long userId, Priority priority) {
        return noteRepository.findByUserIdAndPriority(userId, priority);
    }

    @Override
    public List<Note> findNotesByUserIdAndCompanyIdAndPriority(Long userId, Long companyId, Priority priority) {
        return noteRepository.findByUserIdAndCompany_IdAndPriority(userId, companyId, priority);
    }

    @Override
    public List<Note> findNotesByCompanyIdAndPriorityAndStatus(Long companyId, Priority priority, Status status) {
        return noteRepository.findByCompany_IdAndPriorityAndStatus(companyId, priority, status);
    }

    @Override
    public List<Note> findNotesByUserIdAndCompanyIdAndPriorityAndStatus(Long userId, Long companyId, Priority priority, Status status) {
        return noteRepository.findByUserIdAndCompany_IdAndPriorityAndStatus(userId, companyId, priority, status);
    }

    @Override
    public List<Note> findNotesSortedByPriorityAsc(Long companyId) {
        return noteRepository.findByCompany_IdOrderByPriorityAsc(companyId);
    }

    @Override
    public List<Note> findNotesSortedByPriorityDesc(Long companyId) {
        return noteRepository.findByCompany_IdOrderByPriorityDesc(companyId);
    }
    
    @Override
    public List<Note> findNotesByCompanyIdAndVisibilityAndUserId(Long companyId, List<Note.Visibility> visibilities, Long userId) {
        return noteRepository.findNotesByCompanyIdAndVisibilityInAndUserId(companyId, visibilities, userId);
    }

    
    
}
