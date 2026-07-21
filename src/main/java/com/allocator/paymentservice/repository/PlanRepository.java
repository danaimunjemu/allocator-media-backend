package com.allocator.paymentservice.repository;

import com.allocator.paymentservice.entity.Plan;
import com.allocator.paymentservice.enums.PlanTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanRepository extends JpaRepository<Plan, UUID> {
    List<Plan> findByActiveTrue();
    Optional<Plan> findByTierAndActiveTrue(PlanTier tier);
}
