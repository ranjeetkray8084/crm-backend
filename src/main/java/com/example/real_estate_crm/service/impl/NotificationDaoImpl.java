package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.repository.NotificationRepository;
import com.example.real_estate_crm.service.dao.NotificationDao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationDaoImpl implements NotificationDao {

    @Autowired
    private NotificationRepository notificationRepository;

    // Fetch notifications by userId and company (tenant)
    @Override
    public List<Notification> findByUserAndCompany(User user, Company company) {
        return notificationRepository.findByUserAndCompany(user, company);
    }


    // Save/send notification
    @Override
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }
}
