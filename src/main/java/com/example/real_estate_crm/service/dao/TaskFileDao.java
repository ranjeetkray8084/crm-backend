package com.example.real_estate_crm.service.dao;

import com.example.real_estate_crm.model.TaskFile;
import com.example.real_estate_crm.model.User;

import java.util.List;
import java.util.Optional;

public interface TaskFileDao {

    TaskFile save(TaskFile taskFile);

    Optional<TaskFile> findById(Long id);

    List<TaskFile> findByCompanyId(Long companyId);

    List<TaskFile> findByCompanyIdAndAssignedTo(Long companyId, Long userId);

    List<TaskFile> findByUploadedBy(User uploadedBy);  // ✅ updated from Long to User
}
