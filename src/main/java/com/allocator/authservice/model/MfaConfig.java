package com.allocator.authservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "mfa_configs", indexes = {
        @Index(name = "idx_mfa_user", columnList = "user_id", unique = true)
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MfaConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Builder.Default
    @Column(name = "totp_enabled", columnDefinition = "boolean default false")
    private boolean totpEnabled = false;

    @Column(name = "totp_secret", length = 512)
    private String totpSecret;

    @Builder.Default
    @Column(name = "email_mfa_enabled", columnDefinition = "boolean default false")
    private boolean emailMfaEnabled = false;

    @Builder.Default
    @Column(name = "backup_codes_enabled", columnDefinition = "boolean default false")
    private boolean backupCodesEnabled = false;

    @Builder.Default
    @Column(name = "preferred_method")
    private String preferredMethod = "NONE";
}
