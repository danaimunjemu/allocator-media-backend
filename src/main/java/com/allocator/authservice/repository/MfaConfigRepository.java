package com.allocator.authservice.repository;

import com.allocator.authservice.model.MfaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaConfigRepository extends JpaRepository<MfaConfig, UUID> {
    Optional<MfaConfig> findByUserId(UUID userId);
}
