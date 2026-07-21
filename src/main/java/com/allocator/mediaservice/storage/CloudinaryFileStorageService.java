package com.allocator.mediaservice.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Cloudinary-backed storage — used for "staging" so uploads survive Render's
// ephemeral disk across redeploys (see LocalFileStorageService). Every method
// here accepts either a bare Cloudinary public_id or a full delivery URL
// (media.getFilePath() stores whichever generateFileUrl produced at upload
// time), matching the same tolerant-input convention LocalFileStorageService
// already established.
@Service
@Slf4j
@Profile("staging")
public class CloudinaryFileStorageService implements FileStorageService {

    // Matches ".../upload/v<version>/<public_id>.<ext>" — public_id can itself
    // contain slashes (folder path), so this must be non-greedy up to the last dot.
    private static final Pattern PUBLIC_ID_PATTERN = Pattern.compile("/upload/v\\d+/(.+)\\.[a-zA-Z0-9]+$");

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            Map<String, Object> options = ObjectUtils.asMap(
                    "folder", folder != null && !folder.isEmpty() ? folder : "uploads",
                    "resource_type", "auto",
                    "use_filename", false,
                    "unique_filename", true
            );
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), options);
            String secureUrl = (String) result.get("secure_url");
            log.info("Successfully uploaded file to Cloudinary: {}", secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file to Cloudinary", e);
        }
    }

    @Override
    public InputStream downloadFile(String objectName) {
        try {
            String url = objectName.startsWith("http") ? objectName : cloudinary.url().secure(true).generate(objectName);
            return URI.create(url).toURL().openStream();
        } catch (IOException e) {
            log.error("Failed to download file from Cloudinary: {}", objectName, e);
            throw new RuntimeException("Failed to download file from Cloudinary", e);
        }
    }

    @Override
    public void deleteFile(String objectName) {
        try {
            String publicId = extractPublicId(objectName);
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Successfully deleted file from Cloudinary: {}", publicId);
        } catch (Exception e) {
            log.error("Failed to delete file from Cloudinary: {}", objectName, e);
            throw new RuntimeException("Failed to delete file from Cloudinary", e);
        }
    }

    @Override
    public String generatePresignedUrl(String objectName, int expiryMinutes) {
        // Cloudinary "upload" delivery type resources are already public/permanent
        // — no presigning needed, just return the existing delivery URL as-is.
        return objectName.startsWith("http") ? objectName : cloudinary.url().secure(true).generate(objectName);
    }

    @Override
    public boolean fileExists(String objectName) {
        try {
            String url = objectName.startsWith("http") ? objectName : cloudinary.url().secure(true).generate(objectName);
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("HEAD");
            return connection.getResponseCode() == 200;
        } catch (IOException e) {
            return false;
        }
    }

    private String extractPublicId(String pathOrUrl) {
        if (pathOrUrl == null) return "";
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            Matcher matcher = PUBLIC_ID_PATTERN.matcher(pathOrUrl);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return pathOrUrl;
    }
}
