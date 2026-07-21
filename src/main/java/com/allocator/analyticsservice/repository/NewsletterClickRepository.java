package com.allocator.analyticsservice.repository;

import com.allocator.analyticsservice.model.NewsletterClick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NewsletterClickRepository extends JpaRepository<NewsletterClick, UUID> {
}
