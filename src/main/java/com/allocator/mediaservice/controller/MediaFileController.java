package com.allocator.mediaservice.controller;

import com.allocator.mediaservice.storage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.net.URLConnection;

// Only meaningful under local storage — Cloudinary (staging) serves files
// directly from its own CDN, so uploaded URLs never point back at this route.
@RestController
@RequestMapping("/media-service/api/v1/media/files")
@RequiredArgsConstructor
@Slf4j
@Profile("!staging")
public class MediaFileController {

    private final LocalFileStorageService storageService;

    @GetMapping("/**")
    public ResponseEntity<Resource> serveFile(jakarta.servlet.http.HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        // Extract filename after /media-service/api/v1/media/files/
        String prefix = "/media-service/api/v1/media/files/";
        String objectName = requestPath.substring(requestPath.indexOf(prefix) + prefix.length());

        log.info("Serving media file: {}", objectName);
        try {
            InputStream in = storageService.downloadFile(objectName);
            Resource resource = new InputStreamResource(in);
            
            String mimeType = URLConnection.guessContentTypeFromName(objectName);
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to serve file: {}", objectName, e);
            return ResponseEntity.notFound().build();
        }
    }
}
