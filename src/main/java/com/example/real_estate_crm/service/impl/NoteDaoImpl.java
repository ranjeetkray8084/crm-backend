package com.example.real_estate_crm.service.impl;
//

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.real_estate_crm.model.Note;
import com.example.real_estate_crm.repository.NoteRepository;
import com.example.real_estate_crm.service.dao.NoteDao;

import java.util.List;
import java.util.Optional;

@Service
public class NoteDaoImpl implements NoteDao {

    @Autowired
    private NoteRepository noteRepository;

    @Override
    public Note saveNote(Note note) {
        return noteRepository.save(note);
    }

    @Override
    public List<Note> findNotesByUserId(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    @Override
    public List<Note> findNotesVisibleToUser(Long userId) {
        return noteRepository.findByVisibleUserIdsContaining(userId);
    }

    @Override
    public List<Note> findNotesByVisibility(Note.Visibility visibility) {
        return noteRepository.findByVisibility(visibility);
    }

    public List<Note> getNotesForUser(Long userId) {
        // Get notes visible to the user using a repository method that filters notes by visibility
        return noteRepository.findNotesVisibleToUser(userId);
    }

    @Override
    public List<Note> findNotesVisibleToUser1(Long userId) {
        return noteRepository.findNotesVisibleToUser(userId);
    }


   
    @Override
    public Optional<Note> findNoteById(Long noteId) {
        return Optional.ofNullable(noteRepository.findById(noteId).orElse(null)); // Adjust based on your repository
    }
    
    @Override
    public List<Note> findAllNotes() {
        return noteRepository.findAll();
    }

}
