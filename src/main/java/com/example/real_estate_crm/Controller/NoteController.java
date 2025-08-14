package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.*;
import com.example.real_estate_crm.model.Note.Priority;
import com.example.real_estate_crm.model.Note.Status;
import com.example.real_estate_crm.model.Note.Visibility;
import com.example.real_estate_crm.repository.*;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.NoteDao;
import com.example.real_estate_crm.service.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/companies/{companyId}/notes")
public class NoteController {

    private final NoteDao noteDao;
    private final UserDao userService;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final NotificationService notificationService;
    private final NoteRepository noteRepository;
    private final NoteRemarkRepository noteRemarkRepository;

    @Autowired
    public NoteController(
            NoteDao noteDao,
            UserDao userService,
            UserRepository userRepository,
            CompanyRepository companyRepository,
            NotificationService notificationService,
            NoteRepository noteRepository,
            NoteRemarkRepository noteRemarkRepository
    ) {
        this.noteDao = noteDao;
        this.userService = userService;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.notificationService = notificationService;
        this.noteRepository = noteRepository;
        this.noteRemarkRepository = noteRemarkRepository;
    }

    // CREATE NOTE
    @PostMapping
    public ResponseEntity<Note> addNote(@PathVariable Long companyId, @RequestBody Note note) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) return ResponseEntity.badRequest().build();

        note.setCompany(companyOpt.get());
        Note savedNote = noteDao.saveNote(note);

        String username = userService.findUsernameByUserId(note.getUserId()).orElse("Unknown User");

        switch (note.getVisibility()) {
            case ONLY_ME -> {
                // No notifications needed for private notes
            }
            case ME_AND_ADMIN -> {
                // Send notification to all admins in the company (excluding note creator)
                userRepository.findByCompanyIdAndRole(companyId, User.Role.ADMIN)
                        .stream()
                        .filter(admin -> !admin.getUserId().equals(note.getUserId()))
                        .forEach(admin -> notificationService.sendNotification(
                                admin.getUserId(), companyOpt.get(), username + " created a new note for you."));
            }
            case ALL_USERS -> {
                // Send notification to all users in the company (excluding note creator)
                userRepository.findByCompanyIdAndRole(companyId, User.Role.USER)
                        .stream()
                        .filter(user -> !user.getUserId().equals(note.getUserId()))
                        .forEach(user -> notificationService.sendNotification(
                                user.getUserId(), companyOpt.get(), username + " shared a new note for you."));
            }
            case SPECIFIC_USERS -> {
                // Send notification to specific selected users (excluding note creator)
                if (note.getVisibleUserIds() != null) {
                    for (Long userId : note.getVisibleUserIds()) {
                        // Don't send notification to the note creator
                        if (!userId.equals(note.getUserId())) {
                            userRepository.findById(userId).ifPresent(user -> {
                                // Only notify if user belongs to the same company
                                if (user.getCompany() != null && user.getCompany().getId().equals(companyId)) {
                                    notificationService.sendNotification(
                                            user.getUserId(), companyOpt.get(), username + " shared a new note with you.");
                                }
                            });
                        }
                    }
                }
            }
            case ME_AND_DIRECTOR -> {
                // Send notification to all directors in the company (excluding note creator)
                List<User> directors = userRepository.findByCompanyIdAndRole(companyId, User.Role.DIRECTOR);
                
                directors.stream()
                        .filter(director -> !director.getUserId().equals(note.getUserId()))
                        .forEach(director -> {
                            notificationService.sendNotification(
                                    director.getUserId(), companyOpt.get(), username + " shared a new note with you.");
                        });
            }
            case ALL_ADMIN -> {
                // Send notification to all admins in the company (excluding note creator)
                userRepository.findByCompanyIdAndRole(companyId, User.Role.ADMIN)
                        .stream()
                        .filter(admin -> !admin.getUserId().equals(note.getUserId()))
                        .forEach(admin -> notificationService.sendNotification(
                                admin.getUserId(), companyOpt.get(), username + " shared a note for all admins."));
            }
            case SPECIFIC_ADMIN -> {
                // Send notification to specific selected admins (excluding note creator)
                if (note.getVisibleUserIds() != null) {
                    for (Long userId : note.getVisibleUserIds()) {
                        // Don't send notification to the note creator
                        if (!userId.equals(note.getUserId())) {
                            userRepository.findById(userId).ifPresent(user -> {
                                // Only notify if user is admin and belongs to the same company
                                if (user.getRole() == User.Role.ADMIN && 
                                    user.getCompany() != null && 
                                    user.getCompany().getId().equals(companyId)) {
                                    notificationService.sendNotification(
                                            user.getUserId(), companyOpt.get(), username + " shared a note with you.");
                                }
                            });
                        }
                    }
                }
            }
        }
        return ResponseEntity.ok(savedNote);
    }

    // ✅ ONLY THIS METHOD RETAINED
    @GetMapping("/visible-to/{userId}")
    public ResponseEntity<List<Note>> getNotesVisibleToUser(
            @PathVariable Long companyId,
            @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean isAdmin,
            @RequestParam(defaultValue = "false") boolean isDirector) {
        return ResponseEntity.ok(noteDao.findNotesVisibleToUserAndCompany(userId, companyId, isAdmin, isDirector));
    }

    
    
    @GetMapping("/{noteId}")
    public ResponseEntity<Note> getNoteById(@PathVariable Long companyId, @PathVariable Long noteId) {
        return noteDao.findNoteByIdAndCompany(noteId, companyId)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Note>> getNotesByUser(@PathVariable Long companyId, @PathVariable Long userId) {
        return ResponseEntity.ok(noteDao.findNotesByUserIdAndCompanyId(userId, companyId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<Note>> getAllPublicNotes(@PathVariable Long companyId) {
        return ResponseEntity.ok(noteRepository.findAll().stream()
                .filter(note -> note.getVisibility() == Visibility.ALL_USERS
                             && note.getCompany() != null
                             && note.getCompany().getId().equals(companyId))
                .collect(Collectors.toList()));
    }

    @GetMapping("/public-and-admin")
    public ResponseEntity<List<Note>> getAllPublicAndAdminNotes(
            @PathVariable Long companyId,
            @RequestParam Long adminId
    ) {
        // 1. Get assigned users under this admin
        List<User> assignedUsers = userRepository.findByCompanyIdAndAdmin_UserId(companyId, adminId);
        List<Long> userIds = assignedUsers.stream().map(User::getUserId).toList();

        // 2. Fetch notes created by assigned users with public/admin visibility
        List<Visibility> visibilities = List.of(Visibility.ALL_USERS, Visibility.ME_AND_ADMIN);
        List<Note> notes = noteRepository.findAllByCompany_IdAndVisibilityInAndUserIdIn(
                companyId, visibilities, userIds
        );

        // 3. Add ALL_ADMIN notes (from any user)
        List<Note> allAdminNotes = noteRepository.findAllByCompany_IdAndVisibility(
                companyId, Visibility.ALL_ADMIN
        );

        // 4. Add SPECIFIC_ADMIN notes visible to this admin
        List<Note> specificAdminNotes = noteRepository
                .findAllByCompany_IdAndVisibilityAndVisibleUserIdsContaining(
                        companyId, Visibility.SPECIFIC_ADMIN, adminId
                );

        // 5. Combine and avoid duplicates
        Set<Long> existingNoteIds = notes.stream()
                .map(Note::getId)
                .collect(Collectors.toSet());

        Stream.of(allAdminNotes, specificAdminNotes)
                .flatMap(Collection::stream)
                .filter(note -> !existingNoteIds.contains(note.getId()))
                .forEach(notes::add);

        return ResponseEntity.ok(notes);
    }

    /**
     * Get today's events for admin dashboard (including director-created events)
     */
    @GetMapping("/today-events-admin")
    public ResponseEntity<List<Note>> getTodayEventsForAdmin(
            @PathVariable Long companyId,
            @RequestParam Long adminId
    ) {
        try {
            // Get today's date
            LocalDate today = LocalDate.now();
            
            // 1. Get all notes that admins should see (including director-created ones)
            List<Note> allAdminNotes = new ArrayList<>();
            
            // Get notes from assigned users
            List<User> assignedUsers = userRepository.findByCompanyIdAndAdmin_UserId(companyId, adminId);
            List<Long> assignedUserIds = assignedUsers.stream().map(User::getUserId).toList();
            
            if (!assignedUserIds.isEmpty()) {
                List<Visibility> visibilities = List.of(Visibility.ALL_USERS, Visibility.ME_AND_ADMIN);
                allAdminNotes.addAll(noteRepository.findAllByCompany_IdAndVisibilityInAndUserIdIn(
                        companyId, visibilities, assignedUserIds
                ));
            }
            
            // Get ALL_ADMIN notes from any user
            allAdminNotes.addAll(noteRepository.findAllByCompany_IdAndVisibility(
                    companyId, Visibility.ALL_ADMIN
            ));
            
            // Get SPECIFIC_ADMIN notes visible to this admin
            allAdminNotes.addAll(noteRepository.findAllByCompany_IdAndVisibilityAndVisibleUserIdsContaining(
                    companyId, Visibility.SPECIFIC_ADMIN, adminId
            ));
            
            // Get notes created by directors (admins need to see director events)
            List<User> directors = userRepository.findByCompanyIdAndRole(companyId, User.Role.DIRECTOR);
            List<Long> directorIds = directors.stream().map(User::getUserId).toList();
            
            if (!directorIds.isEmpty()) {
                // Get director notes with appropriate visibility
                List<Visibility> directorVisibilities = List.of(
                        Visibility.ALL_USERS, 
                        Visibility.ME_AND_ADMIN, 
                        Visibility.ALL_ADMIN, 
                        Visibility.ME_AND_DIRECTOR
                );
                allAdminNotes.addAll(noteRepository.findAllByCompany_IdAndVisibilityInAndUserIdIn(
                        companyId, directorVisibilities, directorIds
                ));
            }
            
            // 2. Filter for today's events only
            List<Note> todayEvents = allAdminNotes.stream()
                    .filter(note -> note.getDateTime() != null && 
                            note.getStatus() != Status.COMPLETED &&
                            note.getDateTime().toLocalDate().equals(today))
                    .distinct()
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(todayEvents);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }





 // Correct logic for director
    @GetMapping("/director-visible/{directorId}")
    public ResponseEntity<List<Note>> getNotesVisibleToDirector(
            @PathVariable Long companyId,
            @PathVariable Long directorId) {

        List<Note> notes = noteRepository.findNotesVisibleToDirector(companyId, directorId, true);
        return ResponseEntity.ok(notes);
    }


    @PutMapping("/{noteId}")
    public ResponseEntity<Note> updateNote(
            @PathVariable Long companyId, @PathVariable Long noteId, @RequestBody Note noteDetails) {
        Optional<Note> noteOpt = noteDao.findNoteByIdAndCompany(noteId, companyId);
        if (noteOpt.isEmpty()) return ResponseEntity.notFound().build();

        Note existingNote = noteOpt.get();
        if (noteDetails.getContent() != null) existingNote.setContent(noteDetails.getContent());
        if (noteDetails.getVisibility() != null) existingNote.setVisibility(noteDetails.getVisibility());
        if (noteDetails.getVisibleUserIds() != null) existingNote.setVisibleUserIds(noteDetails.getVisibleUserIds());
        if (noteDetails.getDateTime() != null) existingNote.setDateTime(noteDetails.getDateTime());
        if (noteDetails.getPriority() != null) existingNote.setPriority(noteDetails.getPriority());
        if (noteDetails.getStatus() != null) existingNote.setStatus(noteDetails.getStatus());

        return ResponseEntity.ok(noteDao.updateNote(existingNote));
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long companyId, @PathVariable Long noteId) {
        if (noteDao.findNoteByIdAndCompany(noteId, companyId).isEmpty()) return ResponseEntity.notFound().build();
        noteRemarkRepository.deleteByNoteIdAndCompanyId(noteId, companyId);
        noteDao.deleteNoteByIdAndCompany(noteId, companyId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{noteId}/status")
    public ResponseEntity<Note> updateNoteStatus(
            @PathVariable Long companyId, @PathVariable Long noteId, @RequestParam("status") Status status) {
        Optional<Note> optionalNote = noteDao.findNoteByIdAndCompany(noteId, companyId);
        if (optionalNote.isEmpty()) return ResponseEntity.notFound().build();
        Note note = optionalNote.get();
        note.setStatus(status);
        return ResponseEntity.ok(noteDao.updateNote(note));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Note>> getNotesByCompanyAndStatus(
            @PathVariable Long companyId, @PathVariable("status") Status status) {
        return ResponseEntity.ok(noteDao.findNotesByCompanyIdAndStatus(companyId, status));
    }

    @GetMapping("/user/{userId}/status/{status}")
    public ResponseEntity<List<Note>> getNotesByUserAndStatus(
            @PathVariable Long companyId, @PathVariable Long userId, @PathVariable("status") Status status) {
        return ResponseEntity.ok(noteDao.findNotesByUserIdAndCompanyIdAndStatus(userId, companyId, status));
    }

    @GetMapping("/status-only/{status}")
    public ResponseEntity<List<Note>> getNotesByStatusOnly(@PathVariable("status") Status status) {
        return ResponseEntity.ok(noteDao.findNotesByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Note>> getNotesByCompanyAndPriority(
            @PathVariable Long companyId, @PathVariable("priority") Priority priority) {
        return ResponseEntity.ok(noteDao.findNotesByCompanyIdAndPriority(companyId, priority));
    }

    @GetMapping("/user/{userId}/priority/{priority}")
    public ResponseEntity<List<Note>> getNotesByUserAndPriority(
            @PathVariable Long companyId, @PathVariable Long userId,
            @PathVariable("priority") Priority priority) {
        return ResponseEntity.ok(noteDao.findNotesByUserIdAndCompanyIdAndPriority(userId, companyId, priority));
    }

    @GetMapping("/priority/{priority}/status/{status}")
    public ResponseEntity<List<Note>> getNotesByPriorityAndStatus(
            @PathVariable Long companyId,
            @PathVariable("priority") Priority priority,
            @PathVariable("status") Status status) {
        return ResponseEntity.ok(noteDao.findNotesByCompanyIdAndPriorityAndStatus(companyId, priority, status));
    }

    @GetMapping("/user/{userId}/priority/{priority}/status/{status}")
    public ResponseEntity<List<Note>> getNotesByUserAndPriorityAndStatus(
            @PathVariable Long companyId, @PathVariable Long userId,
            @PathVariable("priority") Priority priority, @PathVariable("status") Status status) {
        return ResponseEntity.ok(noteDao.findNotesByUserIdAndCompanyIdAndPriorityAndStatus(userId, companyId, priority, status));
    }

    @GetMapping("/priority/sort/asc")
    public ResponseEntity<List<Note>> getNotesSortedByPriorityAsc(@PathVariable Long companyId) {
        return ResponseEntity.ok(noteDao.findNotesSortedByPriorityAsc(companyId));
    }

    @GetMapping("/priority/sort/desc")
    public ResponseEntity<List<Note>> getNotesSortedByPriorityDesc(@PathVariable Long companyId) {
        return ResponseEntity.ok(noteDao.findNotesSortedByPriorityDesc(companyId));
    }

    @PatchMapping("/{noteId}/priority")
    public ResponseEntity<Note> updateNotePriority(
            @PathVariable Long companyId,
            @PathVariable Long noteId,
            @RequestParam("priority") Priority priority) {

        Optional<Note> optionalNote = noteDao.findNoteByIdAndCompany(noteId, companyId);
        if (optionalNote.isEmpty()) return ResponseEntity.notFound().build();

        Note note = optionalNote.get();
        note.setPriority(priority);
        return ResponseEntity.ok(noteDao.updateNote(note));
    }

    @PostMapping("/{noteId}/remarks")
    public ResponseEntity<?> addRemarkToNote(
            @PathVariable Long noteId, @RequestBody Map<String, String> requestBody) {

        Optional<Note> optionalNote = noteRepository.findById(noteId);
        if (optionalNote.isEmpty()) return ResponseEntity.notFound().build();

        String userIdStr = requestBody.get("userId");
        String remarkText = requestBody.get("remark");
        if (remarkText == null || remarkText.trim().isEmpty()) return ResponseEntity.badRequest().body("Remark cannot be empty");
        if (userIdStr == null) return ResponseEntity.badRequest().body("User ID is required");

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body("Invalid User ID");
        }

        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) return ResponseEntity.badRequest().body("User not found");

        NoteRemark remark = new NoteRemark();
        remark.setRemark(remarkText);
        remark.setNote(optionalNote.get());
        remark.setCreatedBy(optionalUser.get());

        noteRemarkRepository.save(remark);
        return ResponseEntity.ok("Remark added successfully");
    }

    @GetMapping("/{noteId}/remarks")
    public ResponseEntity<?> getRemarksByNoteId(@PathVariable Long noteId) {
        if (!noteRepository.existsById(noteId)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(noteRemarkRepository.findByNote_Id(noteId));
    }

    @GetMapping("/{noteId}/visible-users")
    public ResponseEntity<List<Long>> getVisibleUserIdsForNote(
            @PathVariable Long companyId, @PathVariable Long noteId) {
        Optional<Note> noteOpt = noteRepository.findById(noteId);
        if (noteOpt.isEmpty()) return ResponseEntity.notFound().build();
        Note note = noteOpt.get();
        if (!note.getCompany().getId().equals(companyId)) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(note.getVisibility() == Visibility.SPECIFIC_USERS ? note.getVisibleUserIds() : List.of());
    }

    @GetMapping("/all/{userId}")
    public ResponseEntity<?> getAllNotesForUser(
            @PathVariable Long companyId, @PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean isAdmin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isEmpty()) return ResponseEntity.badRequest().body("Invalid company ID");

        List<Note> userNotes = noteDao.findNotesByUserIdAndCompanyId(userId, companyId);
        List<Note> visibleNotes = noteDao.findNotesVisibleToUserAndCompany(userId, companyId, isAdmin);
        List<Note> publicNotes = noteDao.findNotesByVisibilityInCompany(companyId, List.of(Visibility.ALL_USERS));

        Map<Long, Note> uniqueNotes = new HashMap<>();
        userNotes.forEach(n -> uniqueNotes.put(n.getId(), n));
        visibleNotes.forEach(n -> uniqueNotes.put(n.getId(), n));
        publicNotes.forEach(n -> uniqueNotes.put(n.getId(), n));

        List<Note> allNotes = new ArrayList<>(uniqueNotes.values());
        allNotes.sort(Comparator.comparing(Note::getCreatedAt).reversed());

        int fromIndex = Math.min(page * size, allNotes.size());
        int toIndex = Math.min(fromIndex + size, allNotes.size());
        List<Note> pagedNotes = allNotes.subList(fromIndex, toIndex);

        String username = userService.findUsernameByUserId(userId).orElse("Unknown User");

        Map<String, Object> response = new HashMap<>();
        response.put("username", username);
        response.put("notes", pagedNotes);
        response.put("total", allNotes.size());
        response.put("page", page);
        response.put("size", size);

        return ResponseEntity.ok(response);
    }
}
