package com.allocator.authservice.repository;

import com.allocator.authservice.model.PasswordResetToken;
import com.allocator.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, java.util.UUID> {
    Optional<PasswordResetToken> findByToken(String token);

    int deleteByUser(User user);
}
