package com.example.real_estate_crm.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.User.Role;
import com.example.real_estate_crm.model.Note;
import com.example.real_estate_crm.repository.NoteRepository;
import com.example.real_estate_crm.repository.UserRepository;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.NoteDao;
import com.example.real_estate_crm.service.impl.UserDaoImpl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired
    private NoteDao noteDao;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDaoImpl userService;

    @Autowired
    UserRepository userRepository;
    
    @Autowired
    NoteRepository noteRepository;

    // API to save a new note
    @PostMapping
    public Note saveNote(@RequestBody Note note) {
        // Save the note first
        Note savedNote = noteDao.saveNote(note);

        // Safely fetch the username of the note creator
        Optional<String> usernameOpt = userService.findUsernameByUserId(note.getUserId());

        // Handle case when username is not found
        String username = usernameOpt.orElse("Unknown User");

        // Handle notifications based on visibility
        switch (note.getVisibility()) {

            case ME_AND_ADMIN:
                List<User> admins = userRepository.findByRole(Role.ADMIN); // Fetch all admin users
                for (User admin : admins) {
                    notificationService.sendNotification(admin.getUserId(), username + " created a new note for admin.");
                }
                break;

            case ALL_USERS:
                // Notify all users with 'USER' role
                List<User> usersWithRoleUser = userRepository.findByRole(Role.USER);  // Fetch users with 'USER' role
                for (User user : usersWithRoleUser) {
                    notificationService.sendNotification(user.getUserId(), username + " shared a public note.");
                }
                break;

            case SPECIFIC_USERS:
                // Notify only specific users listed in 'note.getVisibleUserIds()'
                if (note.getVisibleUserIds() != null && !note.getVisibleUserIds().isEmpty()) {
                    // Iterate through the visible user IDs
                    for (Long userId : note.getVisibleUserIds()) {
                        // Fetch the user by ID to check their role (optional)
                        Optional<User> userOpt = userRepository.findById(userId);

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();

                            // You can add extra filtering logic here if needed
                            // For example, notify only users with the ADMIN role
                            if (user.getRole() == Role.ADMIN || user.getRole() == Role.USER) {
                                notificationService.sendNotification(user.getUserId(), username + " shared a private note with you.");
                            }
                        }
                    }
                }
                break;

            default:
                // No notification for ONLY_ME
                break;
        }

        return savedNote;
    }

    // API to get all notes by user (optional)
    @GetMapping("/user/{userId}")
    public List<Note> getNotesByUser(@PathVariable Long userId) {
        return noteDao.findNotesByUserId(userId);
    }

    // New API to get the users who can see a specific note based on its visibility
    @GetMapping("/{noteId}/visibility")
    public List<Long> getNoteVisibility(@PathVariable Long noteId) {
        Note note = noteDao.findNotesByUserId(noteId).stream()
                .filter(n -> n.getId().equals(noteId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Note not found"));

        switch (note.getVisibility()) {
            case ONLY_ME:
                return List.of(note.getUserId());  // Only the creator can see the note
            case ME_AND_ADMIN:
                return List.of(note.getUserId(), 1L);  // User and Admin (Assuming Admin has ID 1)
            case ALL_USERS:
                return getAllUserIds();  // All users can see the note
            case SPECIFIC_USERS:
                return note.getVisibleUserIds();  // Only specific users can see the note
            default:
                throw new RuntimeException("Unknown visibility type");
        }
    }

    
    @GetMapping("/public-and-admin")
    public List<Note> getAllPublicAndAdminNotes() {
        // Getting notes where visibility is either ALL_USERS or ME_AND_ADMIN
        return noteRepository.findByVisibilityIn(
                List.of(Note.Visibility.ALL_USERS, Note.Visibility.ME_AND_ADMIN)
        );
    }
    
    
    @GetMapping("/public")
    public List<Note> getAllPublicNotes() {
        List<Note> allNotes = noteRepository.findAll();
        return allNotes.stream()
                .filter(note -> note.getVisibility() == Note.Visibility.ALL_USERS)
                .collect(Collectors.toList());
    }
    // Fake method to simulate fetching all users
    private List<Long> getAllUserIds() {
        // TODO: Replace with real user fetching (e.g., from UserService)
        return List.of(2L, 3L, 4L, 5L); // example users
    }

    @GetMapping("/visible-to/{userId}")
    public List<Note> getNotesVisibleToUser(@PathVariable Long userId) {
        return noteDao.findNotesVisibleToUser(userId);
    }
}
