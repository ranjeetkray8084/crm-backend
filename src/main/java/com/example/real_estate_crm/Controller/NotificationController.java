package com.example.real_estate_crm.Controller;

import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.service.NotificationService;
import com.example.real_estate_crm.service.dao.UserDao;
import com.example.real_estate_crm.repository.CompanyRepository;
import com.example.real_estate_crm.repository.NotificationRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationRepository repo;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private CompanyRepository companyRepository;

    private Company getCompany(Long companyId) {
        return companyRepository.findById(companyId).orElse(null);
    }

    private User getUser(Long userId) {
        return userDao.findById(userId).orElse(null);
    }

    @GetMapping("/user/{userId}/company/{companyId}/unread")
    public ResponseEntity<List<Notification>> getUnreadByUserAndCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Notification> notifications = repo.findByUserAndCompany(user, company)
                                               .stream()
                                               .filter(n -> !n.isRead())
                                               .toList();

        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/user/{userId}/company/{companyId}/unread-count")
    public ResponseEntity<Long> getUnreadCountByUserAndCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(0L);
        }

        long count = repo.findByUserAndCompany(user, company)
                         .stream()
                         .filter(n -> !n.isRead())
                         .count();

        return ResponseEntity.ok(count);
    }

    @GetMapping("/user/{userId}/company/{companyId}")
    public ResponseEntity<List<Notification>> getNotificationsByUserAndCompany(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Notification> notifications = notificationService.getNotificationsByUserAndCompany(user, company);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/mark-all-as-read/user/{userId}/company/{companyId}")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Long userId,
            @PathVariable Long companyId) {
        Company company = getCompany(companyId);
        User user = getUser(userId);

        if (company == null || user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Company or User not found");
        }

        notificationService.markAllAsRead(user, company);
        return ResponseEntity.ok("All notifications marked as read.");
    }

    @PostMapping("/mark-as-read/{id}")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        Notification notif = repo.findById(id).orElse(null);
        if (notif == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found");
        }
        notif.setIsRead(true);
        repo.save(notif);
        return ResponseEntity.ok(notif);
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendNotification(
            @RequestParam Long actorUserId,
            @RequestParam Long companyId,
            @RequestParam String message) {

        Company company = getCompany(companyId);
        User actor = getUser(actorUserId);

        if (company == null || actor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Actor user or Company not found.");
        }

        // 🔔 Notify Admin (if actor is USER)
        if (actor.getRole() == User.Role.USER && actor.getAdmin() != null) {
            notificationService.sendNotification(actor.getAdmin().getUserId(), company, message);
        }

        // 🔔 Notify Director
        User director = userDao.findDirectorByCompany(company);
        if (director != null) {
            notificationService.sendNotification(director.getUserId(), company, message);
        }

        return ResponseEntity.ok("Notification sent to relevant users.");
    }

}
