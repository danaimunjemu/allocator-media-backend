package com.allocator.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "backup_codes", indexes = {
        @Index(name = "idx_backup_user", columnList = "user_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BackupCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Builder.Default
    @Column(name = "used", columnDefinition = "boolean default false")
    private boolean used = false;

    @Column(name = "used_at")
    private LocalDateTime usedAt;
}
