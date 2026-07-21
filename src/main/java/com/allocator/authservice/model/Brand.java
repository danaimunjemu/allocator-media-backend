package com.allocator.authservice.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "brands")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Brand {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String website;
    private String contactEmail;
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BrandStatus status = BrandStatus.ACTIVE;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "name",  column = @Column(name = "contact_person_name")),
        @AttributeOverride(name = "role",  column = @Column(name = "contact_person_role")),
        @AttributeOverride(name = "email", column = @Column(name = "contact_person_email")),
        @AttributeOverride(name = "phone", column = @Column(name = "contact_person_phone")),
    })
    private ContactPerson contactPerson;
}
