package com.allocator.authservice.repository;

import com.allocator.authservice.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {
    List<UserSession> findByUserIdAndActiveTrue(UUID userId);
    List<UserSession> findByUserId(UUID userId);
    Optional<UserSession> findBySessionToken(String sessionToken);
    Optional<UserSession> findFirstByUserIdOrderByLastActiveDesc(UUID userId);
    void deleteByUserId(UUID userId);
}
