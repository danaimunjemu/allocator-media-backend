package com.allocator.authservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_username", columnList = "username")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(length = 1000)
    private String bio;

    @Column(name = "contact_info")
    private String contactInfo;

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    @Column(name = "must_change_password", columnDefinition = "boolean default false")
    private boolean mustChangePassword = false;

    @Builder.Default
    @Column(name = "mfa_enabled", columnDefinition = "boolean default false")
    private boolean mfaEnabled = false;

    @Builder.Default
    @Column(name = "email_verified", columnDefinition = "boolean default true")
    private boolean emailVerified = true;
}
