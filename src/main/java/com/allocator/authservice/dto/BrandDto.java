package com.allocator.authservice.dto;

import com.allocator.authservice.model.BrandStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrandDto {

    private UUID id;
    private String name;
    private String code;
    private String logoUrl;
    private String description;
    private String website;
    private String contactEmail;
    private String contactPhone;
    private BrandStatus status;
    private long userCount;
    private ContactPersonDto contactPerson;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactPersonDto {
        private String name;
        private String role;
        private String email;
        private String phone;
    }
}
