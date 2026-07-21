package com.allocator.authservice.repository;

import com.allocator.authservice.model.EmailVerificationToken;
import com.allocator.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, java.util.UUID> {
    Optional<EmailVerificationToken> findByToken(String token);

    int deleteByUser(User user);
}
