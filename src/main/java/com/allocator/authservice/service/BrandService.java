package com.allocator.authservice.service;

import com.allocator.authservice.dto.BrandDto;
import com.allocator.authservice.dto.UserDto;
import com.allocator.authservice.model.Brand;
import com.allocator.authservice.model.BrandStatus;
import com.allocator.authservice.model.ContactPerson;
import com.allocator.authservice.model.User;
import com.allocator.authservice.repository.BrandRepository;
import com.allocator.authservice.repository.UserRoleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final UserRoleRepository userRoleRepository;

    public List<BrandDto> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public BrandDto getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));
        return mapToDto(brand);
    }

    @Transactional
    public BrandDto createBrand(BrandDto request) {
        Brand brand = Brand.builder()
                .name(request.getName())
                .code(request.getCode().toUpperCase())
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .website(request.getWebsite())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .status(request.getStatus() != null ? request.getStatus() : BrandStatus.ACTIVE)
                .contactPerson(toModel(request.getContactPerson()))
                .build();
        return mapToDto(brandRepository.save(brand));
    }

    @Transactional
    public BrandDto updateBrand(UUID id, BrandDto request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));

        if (request.getName() != null)         brand.setName(request.getName());
        if (request.getCode() != null)         brand.setCode(request.getCode().toUpperCase());
        if (request.getLogoUrl() != null)      brand.setLogoUrl(request.getLogoUrl());
        if (request.getDescription() != null)  brand.setDescription(request.getDescription());
        if (request.getWebsite() != null)      brand.setWebsite(request.getWebsite());
        if (request.getContactEmail() != null) brand.setContactEmail(request.getContactEmail());
        if (request.getContactPhone() != null) brand.setContactPhone(request.getContactPhone());
        if (request.getStatus() != null)       brand.setStatus(request.getStatus());
        if (request.getContactPerson() != null) brand.setContactPerson(toModel(request.getContactPerson()));

        return mapToDto(brandRepository.save(brand));
    }

    @Transactional
    public BrandDto archiveBrand(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + id));
        brand.setStatus(brand.getStatus() == BrandStatus.ARCHIVED ? BrandStatus.ACTIVE : BrandStatus.ARCHIVED);
        return mapToDto(brandRepository.save(brand));
    }

    private BrandDto mapToDto(Brand brand) {
        long userCount = userRoleRepository.countDistinctUserByBrand(brand);
        return BrandDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .code(brand.getCode())
                .logoUrl(brand.getLogoUrl())
                .description(brand.getDescription())
                .website(brand.getWebsite())
                .contactEmail(brand.getContactEmail())
                .contactPhone(brand.getContactPhone())
                .status(brand.getStatus())
                .userCount(userCount)
                .contactPerson(toDto(brand.getContactPerson()))
                .build();
    }

    private ContactPerson toModel(BrandDto.ContactPersonDto dto) {
        if (dto == null) return null;
        return ContactPerson.builder()
                .name(dto.getName())
                .role(dto.getRole())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .build();
    }

    public List<UserDto> getUsersByBrand(UUID brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new EntityNotFoundException("Brand not found: " + brandId));
        return userRoleRepository.findDistinctUsersByBrand(brand).stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getFirstName() + " " + user.getLastName())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .status(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                .createdAt(user.getCreatedAt())
                .build();
    }

    private BrandDto.ContactPersonDto toDto(ContactPerson cp) {
        if (cp == null) return null;
        return BrandDto.ContactPersonDto.builder()
                .name(cp.getName())
                .role(cp.getRole())
                .email(cp.getEmail())
                .phone(cp.getPhone())
                .build();
    }
}
