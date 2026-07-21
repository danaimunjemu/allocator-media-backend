package com.allocator.contentservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "authors", indexes = {
    @Index(name = "idx_authors_user_id", columnList = "user_id"),
    @Index(name = "idx_authors_email", columnList = "email")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Author extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 100)
    private String role; // Editor / Analyst / Author / SUPER_ADMIN etc.

    @Column(name = "contact_info")
    private String contactInfo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_links", columnDefinition = "jsonb")
    private Map<String, String> socialLinks;

    // Links this author profile to a platform user account (nullable for external/anonymous authors)
    @Column(name = "user_id", unique = true)
    private UUID userId;
}
