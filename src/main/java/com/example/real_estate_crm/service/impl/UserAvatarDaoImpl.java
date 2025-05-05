package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.UserAvatar;
import com.example.real_estate_crm.repository.UserAvatarRepository;
import com.example.real_estate_crm.service.dao.UserAvatarDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserAvatarDaoImpl implements UserAvatarDao {

    @Autowired
    private UserAvatarRepository userAvatarRepository;

    @Override
    public void saveUserAvatar(Long userId, byte[] avatar, String avatarName) {
        // Create a new UserAvatar instance
        UserAvatar userAvatar = new UserAvatar();
        userAvatar.setUserId(userId);
        userAvatar.setImage(avatar);
        userAvatar.setAvatarName(avatarName); // Set the avatar name

        // Save the avatar to the database
        userAvatarRepository.save(userAvatar);
    }
    
    @Override
    public UserAvatar getUserAvatarByUserId(Long userId) {
        return userAvatarRepository.findFirstByUserIdOrderByIdDesc(userId);
    }

}
