package com.allocator.authservice.repository;

import com.allocator.authservice.model.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoginHistoryRepository extends JpaRepository<LoginHistory, UUID> {
    List<LoginHistory> findByUserIdOrderByLoginAtDesc(UUID userId);
    List<LoginHistory> findByEmailOrderByLoginAtDesc(String email);
}
