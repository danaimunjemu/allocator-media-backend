package com.allocator.brokerservice.repository;

import com.allocator.brokerservice.entity.Broker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Repository("brokerRepository")
public interface BrokerRepository extends JpaRepository<Broker, UUID>, JpaSpecificationExecutor<Broker> {
    Optional<Broker> findByIdAndActiveTrue(UUID id);
}

