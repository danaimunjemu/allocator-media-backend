package com.allocator.authservice.repository;

import com.allocator.authservice.model.BackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, UUID> {
    List<BackupCode> findByUserIdAndUsedFalse(UUID userId);
    List<BackupCode> findByUserId(UUID userId);
    void deleteByUserId(UUID userId);
}
