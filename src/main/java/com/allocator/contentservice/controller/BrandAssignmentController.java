package com.allocator.contentservice.controller;

import com.allocator.contentservice.dto.ApiResponse;
import com.allocator.contentservice.model.BrandAssignment;
import com.allocator.contentservice.service.BrandAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/brand-assignments")
@RequiredArgsConstructor
@Slf4j
public class BrandAssignmentController {

    private final BrandAssignmentService brandAssignmentService;

    @PostMapping
    public ResponseEntity<ApiResponse<BrandAssignment>> assign(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") UUID requesterId,
            @RequestHeader("X-User-Roles") String userRoles) {

        requireAdminRole(userRoles);
        UUID userId = UUID.fromString(body.get("userId"));
        UUID brandId = UUID.fromString(body.get("brandId"));
        String role = body.get("role");

        BrandAssignment assignment = brandAssignmentService.assign(userId, brandId, role, requesterId);
        return ResponseEntity.ok(ApiResponse.success(assignment, "Brand assignment created"));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> revoke(
            @RequestParam UUID userId,
            @RequestParam UUID brandId,
            @RequestHeader("X-User-Roles") String userRoles) {

        requireAdminRole(userRoles);
        brandAssignmentService.revoke(userId, brandId);
        return ResponseEntity.ok(ApiResponse.success(null, "Brand assignment revoked"));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<BrandAssignment>>> getForUser(
            @PathVariable UUID userId,
            @RequestHeader("X-User-Roles") String userRoles) {

        requireAdminRole(userRoles);
        return ResponseEntity.ok(ApiResponse.success(brandAssignmentService.getAssignmentsForUser(userId)));
    }

    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<List<BrandAssignment>>> getForBrand(
            @PathVariable UUID brandId,
            @RequestHeader("X-User-Roles") String userRoles) {

        requireAdminRole(userRoles);
        return ResponseEntity.ok(ApiResponse.success(brandAssignmentService.getAssignmentsForBrand(brandId)));
    }

    private void requireAdminRole(String userRoles) {
        if (userRoles == null) throw new IllegalStateException("Unauthorized");
        boolean isAdmin = java.util.Arrays.stream(userRoles.split(","))
                .map(String::trim)
                .map(r -> r.startsWith("ROLE_") ? r.substring(5) : r)
                .anyMatch(r -> r.equals("ADMIN") || r.equals("SUPER_ADMIN"));
        if (!isAdmin) throw new IllegalStateException("Only ADMIN or SUPER_ADMIN can manage brand assignments");
    }
}
