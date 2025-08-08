package com.example.real_estate_crm.service.impl;

import com.example.real_estate_crm.model.TaskFile;
import com.example.real_estate_crm.model.User;
import com.example.real_estate_crm.repository.TaskFileRepository;
import com.example.real_estate_crm.service.dao.TaskFileDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskFileDaoImpl implements TaskFileDao {

    private final TaskFileRepository taskFileRepository;

    @Override
    public TaskFile save(TaskFile taskFile) {
        return taskFileRepository.save(taskFile);
    }

    @Override
    public Optional<TaskFile> findById(Long id) {
        return taskFileRepository.findById(id);
    }

    @Override
    public List<TaskFile> findByCompanyId(Long companyId) {
        return taskFileRepository.findByCompanyId(companyId);
    }

    @Override
    public List<TaskFile> findByCompanyIdAndAssignedTo(Long companyId, Long userId) {
        return taskFileRepository.findByCompanyIdAndAssignedTo(companyId, userId);
    }

    @Override
    public List<TaskFile> findByUploadedBy(User uploadedBy) {
        return taskFileRepository.findByUploadedBy(uploadedBy); // ✅ Corrected to use User object
    }
}
