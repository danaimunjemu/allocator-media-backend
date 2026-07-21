package com.allocator.contentservice.service;

import com.allocator.contentservice.model.BrandAssignment;
import com.allocator.contentservice.model.BrandRole;
import com.allocator.contentservice.repository.BrandAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrandAssignmentService {

    private final BrandAssignmentRepository brandAssignmentRepository;

    @Transactional
    public BrandAssignment assign(UUID userId, UUID brandId, String role, UUID assignedBy) {
        if (brandAssignmentRepository.existsByUserIdAndBrandId(userId, brandId)) {
            BrandAssignment existing = brandAssignmentRepository.findByUserIdAndBrandId(userId, brandId)
                    .orElseThrow();
            existing.setRole(role);
            log.info("Updated brand assignment: user={} brand={} role={}", userId, brandId, role);
            return brandAssignmentRepository.save(existing);
        }
        BrandAssignment assignment = BrandAssignment.builder()
                .userId(userId)
                .brandId(brandId)
                .role(role)
                .assignedBy(assignedBy)
                .build();
        log.info("Created brand assignment: user={} brand={} role={}", userId, brandId, role);
        return brandAssignmentRepository.save(assignment);
    }

    @Transactional
    public void revoke(UUID userId, UUID brandId) {
        brandAssignmentRepository.deleteByUserIdAndBrandId(userId, brandId);
        log.info("Revoked brand assignment: user={} brand={}", userId, brandId);
    }

    @Transactional(readOnly = true)
    public List<BrandAssignment> getAssignmentsForUser(UUID userId) {
        return brandAssignmentRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<BrandAssignment> getAssignmentsForBrand(UUID brandId) {
        return brandAssignmentRepository.findByBrandId(brandId);
    }

    @Transactional(readOnly = true)
    public boolean isAssigned(UUID userId, UUID brandId) {
        return brandAssignmentRepository.existsByUserIdAndBrandId(userId, brandId);
    }

    @Transactional(readOnly = true)
    public boolean hasMinimumBrandRole(UUID userId, UUID brandId, BrandRole minimumRole) {
        return brandAssignmentRepository.findByUserIdAndBrandId(userId, brandId)
                .map(a -> {
                    BrandRole actual = BrandRole.fromString(a.getRole());
                    return actual != null && actual.hasMinimum(minimumRole);
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<BrandRole> getBrandRole(UUID userId, UUID brandId) {
        return brandAssignmentRepository.findByUserIdAndBrandId(userId, brandId)
                .map(a -> BrandRole.fromString(a.getRole()));
    }
}
