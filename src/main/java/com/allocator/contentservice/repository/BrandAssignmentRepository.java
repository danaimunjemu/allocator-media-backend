package com.allocator.contentservice.repository;

import com.allocator.contentservice.model.BrandAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BrandAssignmentRepository extends JpaRepository<BrandAssignment, UUID> {

    Optional<BrandAssignment> findByUserIdAndBrandId(UUID userId, UUID brandId);

    boolean existsByUserIdAndBrandId(UUID userId, UUID brandId);

    List<BrandAssignment> findByUserId(UUID userId);

    List<BrandAssignment> findByBrandId(UUID brandId);

    void deleteByUserIdAndBrandId(UUID userId, UUID brandId);

    @Query("SELECT ba FROM BrandAssignment ba WHERE ba.brandId = :brandId AND ba.role = :role")
    List<BrandAssignment> findByBrandIdAndRole(@Param("brandId") UUID brandId, @Param("role") String role);

    @Query("SELECT COUNT(ba) > 0 FROM BrandAssignment ba WHERE ba.userId = :userId AND ba.brandId = :brandId AND ba.role = :role")
    boolean existsByUserIdAndBrandIdAndRole(@Param("userId") UUID userId, @Param("brandId") UUID brandId, @Param("role") String role);
}
