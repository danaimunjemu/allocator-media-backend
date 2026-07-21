package com.allocator.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.UUID;
import com.allocator.notificationservice.service.TrackingService;

@RestController
@RequestMapping("/tracking")
@RequiredArgsConstructor
public class EmailTrackingController {

    private final TrackingService service;

    @GetMapping(value = "/open/{notificationId}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] trackOpen(@PathVariable UUID notificationId) {
        service.recordOpen(notificationId);

        // Return a 1x1 transparent PNG pixel
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x08, (byte) 0xD7, 0x63, 0x60, 0x00, 0x02,
                0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x26, (byte) 0xE5, 0x02, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
                0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }

    @GetMapping("/click/{notificationId}")
    public RedirectView trackClick(@PathVariable UUID notificationId, @RequestParam String url) {
        service.recordClick(notificationId);
        return new RedirectView(url);
    }
}
