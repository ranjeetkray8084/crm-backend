package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.TaskFile;
import com.example.real_estate_crm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskFileRepository extends JpaRepository<TaskFile, Long> {

    // 🔍 Find all files assigned to a specific userId
    List<TaskFile> findByAssignedTo(Long userId);

    // 🔍 Find all files uploaded by a specific User (admin, etc.)
    List<TaskFile> findByUploadedBy(User uploadedBy);

    // 🔍 All files by company
    List<TaskFile> findByCompanyId(Long companyId);

    // 🔍 Files by company and assigned user
    List<TaskFile> findByCompanyIdAndAssignedTo(Long companyId, Long userId);

    // 🔍 For secure preview access (company-level check)
    Optional<TaskFile> findByIdAndCompanyId(Long taskId, Long companyId);

    // 🔍 For dashboard list (latest first)
    List<TaskFile> findByCompanyIdAndAssignedToOrderByUploadDateDesc(Long companyId, Long userId);

    List<TaskFile> findByCompanyIdOrderByUploadDateDesc(Long companyId);

	List<TaskFile> findByCompanyIdAndUploadedBy_UserIdOrderByUploadDateDesc(Long companyId, Long uploadedById);
}
