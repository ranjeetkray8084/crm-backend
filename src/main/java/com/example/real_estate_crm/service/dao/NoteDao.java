package com.example.real_estate_crm.service.dao;
//
//
//


import java.util.List;
import java.util.Optional;

import com.example.real_estate_crm.model.Note;
import com.example.real_estate_crm.model.Note.Visibility;

public interface NoteDao {

    Note saveNote(Note note);

    List<Note> findNotesByUserId(Long userId);

    List<Note> findNotesVisibleToUser(Long userId);

	List<Note> findNotesByVisibility(Visibility visibility);
	
	
	List<Note> findAllNotes();

	List<Note> findNotesVisibleToUser1(Long userId);
	Optional<Note> findNoteById(Long noteId);

}
