package com.example.real_estate_crm.repository;

import com.example.real_estate_crm.model.NoteRemark;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoteRemarkRepository extends JpaRepository<NoteRemark, Long> {

    // Fetch all remarks by note ID
    List<NoteRemark> findByNote_Id(Long noteId);

	@Modifying
    @Transactional
    @Query("DELETE FROM NoteRemark nr WHERE nr.note.id = :noteId AND nr.note.company.id = :companyId")
    void deleteByNoteIdAndCompanyId(@Param("noteId") Long noteId, @Param("companyId") Long companyId);
}
