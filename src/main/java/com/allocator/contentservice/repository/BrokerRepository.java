package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.Broker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository("contentBrokerRepository")
public interface BrokerRepository extends JpaRepository<Broker, UUID> {
    Optional<Broker> findBySlug(String slug);
}

