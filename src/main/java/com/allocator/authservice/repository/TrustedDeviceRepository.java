package com.allocator.authservice.repository;

import com.allocator.authservice.model.TrustedDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, UUID> {
    List<TrustedDevice> findByUserId(UUID userId);
    Optional<TrustedDevice> findByUserIdAndDeviceFingerprint(UUID userId, String fingerprint);
    void deleteByUserIdAndId(UUID userId, UUID deviceId);
}
