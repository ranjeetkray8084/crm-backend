package com.example.real_estate_crm.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Entity
@Table(name = "company")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    @Column(name = "status")
    private String status;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_admins")
    private Integer maxAdmins;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        this.updatedAt = this.createdAt;
        if (this.maxUsers == null) this.maxUsers = 10;
        if (this.maxAdmins == null) this.maxAdmins = 2;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = ZonedDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDateTime();
    }

    @ManyToOne
    @JoinColumn(name = "developer_id")
    @JsonIgnoreProperties({"company", "users", "leads", "followUps", "avatars", "otpCode", "otpExpiry"})
    private User developer;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<User> users;

    public Company(Long id) {
        this.id = id;
    }
}
