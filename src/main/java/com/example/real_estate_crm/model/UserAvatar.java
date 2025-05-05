package com.example.real_estate_crm.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_avatars")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAvatar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "image", nullable = false, columnDefinition = "LONGBLOB") // ensure column name matches DB
    private byte[] image;

    @Column(name = "avatar_name")
    private String avatarName;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "userId", insertable = false, updatable = false)
    private User user;

    public UserAvatar(byte[] image, Long userId, String avatarName) {
        this.image = image;
        this.userId = userId;
        this.avatarName = avatarName;
    }
}

