package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.Notification;
import com.example.real_estate_crm.model.Company;
import com.example.real_estate_crm.model.User;

import java.util.List;

public interface NotificationDao {

    // ✅ Fetch all notifications for a user in a given company (tenant)
    List<Notification> findByUserAndCompany(User user, Company company);

    // ✅ Save or send a notification
    Notification save(Notification notification);
}
