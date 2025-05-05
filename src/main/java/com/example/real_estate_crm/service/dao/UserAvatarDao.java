package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.UserAvatar;

public interface UserAvatarDao {
    // Save user avatar
    void saveUserAvatar(Long userId, byte[] avatar, String avatarName);

    // Fetch user avatar by userId
    UserAvatar getUserAvatarByUserId(Long userId);
}
