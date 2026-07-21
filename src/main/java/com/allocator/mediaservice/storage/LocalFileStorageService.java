package com.allocator.mediaservice.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

// Writes to the container's local disk — fine for local dev, but that disk is
// ephemeral on most PaaS deployments (e.g. Render), so uploads vanish on every
// redeploy. Not used for "staging" (see CloudinaryFileStorageService); still
// the default everywhere else until other environments are migrated too.
@Service
@Slf4j
@Profile("!staging")
public class LocalFileStorageService implements FileStorageService {

    @Value("${media.local.upload-dir:uploads}")
    private String uploadDir;

    @Value("${media.local.base-url:http://localhost:8080/media-service/api/v1/media/files}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize upload directory!", e);
        }
    }

    public String uploadFile(MultipartFile file, String folder) {
        try {
            String originalFilename = file.getOriginalFilename();
            String fileExtension = getFileExtension(originalFilename);
            String uniqueFilename = generateUniqueFilename(fileExtension);
            String objectName = folder != null && !folder.isEmpty() ? folder + "/" + uniqueFilename : uniqueFilename;

            Path targetLocation = Paths.get(uploadDir).resolve(objectName);
            Files.createDirectories(targetLocation.getParent());
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = generateFileUrl(objectName);
            log.info("Successfully uploaded file to local storage: {}", targetLocation.toString());
            
            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to upload file locally: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file locally", e);
        }
    }

    public InputStream downloadFile(String objectName) {
        try {
            Path targetLocation = Paths.get(uploadDir).resolve(objectName).normalize();
            return Files.newInputStream(targetLocation);
        } catch (IOException e) {
            log.error("Failed to download file from local storage: {}", objectName, e);
            throw new RuntimeException("Failed to download file locally", e);
        }
    }

    public void deleteFile(String objectName) {
        try {
            // Reconstruct path. The objectName usually comes as an absolute URL if used natively, but Media stores objectName or URL depending.
            // Oh wait, media stores filePath which is the full URL? No, in MediaService: storageUrl.substring(storageUrl.lastIndexOf('/') + 1) -> fileName.
            // Wait, minioStorageService used to store the full URL in `storageUrl` but `filePath` stored the `objectName`. 
            // In MediaService: `minioStorageService.uploadFile` returned `storageUrl`. `filePath` was set to `storageUrl`.
            // Wait: `String storageUrl = minioStorageService.uploadFile(...)`.
            // Then `media.setFilePath(storageUrl)` and `media.setStorageUrl(storageUrl)`. Both held the URL.
            // But downloadFile and deleteFile were passed `media.getFilePath()` (the URL).
            // This means we need to extract objectName from the URL before deleting or downloading.
            
            // To be robust, let's extract the objectName from the full URL if it gets passed in.
            String cleanObjectName = extractObjectName(objectName);
            Path targetLocation = Paths.get(uploadDir).resolve(cleanObjectName).normalize();
            Files.deleteIfExists(targetLocation);
            log.info("Successfully deleted file locally: {}", targetLocation.toString());
        } catch (IOException e) {
            log.error("Failed to delete file locally: {}", objectName, e);
            throw new RuntimeException("Failed to delete file locally", e);
        }
    }

    public String generatePresignedUrl(String objectName, int expiryMinutes) {
        // Just return the static URL
        return generateFileUrl(extractObjectName(objectName));
    }

    public boolean fileExists(String objectName) {
        Path targetLocation = Paths.get(uploadDir).resolve(extractObjectName(objectName)).normalize();
        return Files.exists(targetLocation);
    }
    
    // Help method to extract the relative path from the absolute URL that is passed
    private String extractObjectName(String pathOrUrl) {
        if (pathOrUrl == null) return "";
        if (pathOrUrl.startsWith(baseUrl)) {
            return pathOrUrl.substring(baseUrl.length() + 1);
        }
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            // Fallback for old Minio URLs if needed, but best effort
            return pathOrUrl.substring(pathOrUrl.lastIndexOf('/') + 1);
        }
        return pathOrUrl;
    }

    private String generateUniqueFilename(String fileExtension) {
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return timestamp + "_" + uuid + (fileExtension.isEmpty() ? "" : "." + fileExtension);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private String generateFileUrl(String objectName) {
        return baseUrl + "/" + objectName;
    }
}
