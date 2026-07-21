package com.allocator.notificationservice.controller;

import com.allocator.notificationservice.dto.CampaignItemRequest;
import com.allocator.notificationservice.dto.CampaignRequest;
import com.allocator.notificationservice.dto.CampaignResponse;
import com.allocator.notificationservice.service.CampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse createCampaign(@Valid @RequestBody CampaignRequest request,
                                            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return service.createCampaign(request, userId);
    }

    @GetMapping
    public List<CampaignResponse> getAllCampaigns() {
        return service.getAllCampaigns();
    }

    @GetMapping("/{id}")
    public CampaignResponse getCampaign(@PathVariable UUID id) {
        return service.getCampaign(id);
    }

    @PutMapping("/{id}")
    public CampaignResponse updateCampaign(@PathVariable UUID id, @Valid @RequestBody CampaignRequest request) {
        return service.updateCampaign(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCampaign(@PathVariable UUID id) {
        service.deleteCampaign(id);
    }

    @PostMapping("/{id}/items")
    public CampaignResponse addItem(@PathVariable UUID id, @Valid @RequestBody CampaignItemRequest request) {
        return service.addItem(id, request);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public CampaignResponse removeItem(@PathVariable UUID id, @PathVariable UUID itemId,
                                        @RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                        @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.removeItem(id, itemId, userId, userRoles);
    }

    @PutMapping("/{id}/items/reorder")
    public CampaignResponse reorderItems(@PathVariable UUID id, @RequestBody List<UUID> orderedItemIds) {
        return service.reorderItems(id, orderedItemIds);
    }

    @PostMapping("/{id}/submit-review")
    public CampaignResponse submitForReview(@PathVariable UUID id,
                                             @RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                             @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.submitForReview(id, userId, userRoles);
    }

    public record ApproveRequest(LocalDateTime scheduledAt, Boolean sendNow) {}

    @PostMapping("/{id}/approve")
    public CampaignResponse approve(@PathVariable UUID id,
                                     @RequestBody(required = false) ApproveRequest body,
                                     @RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                     @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.approve(id, userId, userRoles,
                body != null ? body.scheduledAt() : null,
                body != null && Boolean.TRUE.equals(body.sendNow()));
    }

    public record RejectRequest(String reason) {}

    @PostMapping("/{id}/reject")
    public CampaignResponse reject(@PathVariable UUID id,
                                    @RequestBody(required = false) RejectRequest body,
                                    @RequestHeader(value = "X-User-Id", required = false) UUID userId,
                                    @RequestHeader(value = "X-User-Roles", required = false) String userRoles) {
        return service.reject(id, userId, userRoles, body != null ? body.reason() : null);
    }

    @PostMapping("/{id}/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendCampaign(@PathVariable UUID id) {
        service.sendCampaign(id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable UUID id) {
        byte[] pdf = service.exportPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"newsletter-" + id + ".pdf\"")
                .body(pdf);
    }
}
