package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.NewsletterOpen;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NewsletterOpenRepository extends JpaRepository<NewsletterOpen, UUID> {
    List<NewsletterOpen> findByEmailOrderByOpenedAtDesc(String email);
    long countByEmail(String email);
}
