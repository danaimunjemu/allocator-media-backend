package com.allocator.mediaservice.service;

import com.allocator.mediaservice.storage.LocalFileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailService {

    private final LocalFileStorageService localFileStorageService;

    private static final int THUMBNAIL_WIDTH = 300;
    private static final int MEDIUM_WIDTH = 800;

    public ThumbnailResult generateThumbnails(MultipartFile originalFile, String fileName) throws IOException {
        if (!isImageFile(originalFile)) {
            return ThumbnailResult.builder()
                    .thumbnailUrl(null)
                    .mediumUrl(null)
                    .build();
        }

        try {
            // Read original image
            BufferedImage originalImage = ImageIO.read(originalFile.getInputStream());
            if (originalImage == null) {
                log.warn("Could not read image from file: {}", fileName);
                return ThumbnailResult.builder()
                        .thumbnailUrl(null)
                        .mediumUrl(null)
                        .build();
            }

            // Generate thumbnail (300px)
            String thumbnailFileName = generateThumbnailFileName(fileName, "thumb");
            String thumbnailUrl = generateThumbnail(originalImage, thumbnailFileName, THUMBNAIL_WIDTH);

            // Generate medium (800px)
            String mediumFileName = generateThumbnailFileName(fileName, "medium");
            String mediumUrl = generateThumbnail(originalImage, mediumFileName, MEDIUM_WIDTH);

            log.info("Generated thumbnails for file: {} - thumbnail: {}, medium: {}", 
                    fileName, thumbnailUrl, mediumUrl);

            return ThumbnailResult.builder()
                    .thumbnailUrl(thumbnailUrl)
                    .mediumUrl(mediumUrl)
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate thumbnails for file: {}", fileName, e);
            throw new IOException("Failed to generate thumbnails", e);
        }
    }

    private String generateThumbnail(BufferedImage originalImage, String fileName, int targetWidth) throws IOException {
        // Generate thumbnail
        ByteArrayOutputStream thumbnailOutputStream = new ByteArrayOutputStream();
        
        Thumbnails.of(originalImage)
                .width(targetWidth)
                .keepAspectRatio(true)
                .outputFormat("jpg")
                .outputQuality(0.85)
                .toOutputStream(thumbnailOutputStream);

        // Convert to InputStream for upload
        byte[] thumbnailBytes = thumbnailOutputStream.toByteArray();
        ByteArrayInputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailBytes);

        // Create a mock MultipartFile for the thumbnail
        MultipartFile thumbnailFile = new MockMultipartFile(
                fileName,
                fileName,
                "image/jpeg",
                thumbnailBytes
        );

        // Upload thumbnail to MinIO
        String folder = "thumbnails";
        return localFileStorageService.uploadFile(thumbnailFile, folder);
    }

    private String generateThumbnailFileName(String originalFileName, String suffix) {
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot == -1) {
            return originalFileName + "_" + suffix + ".jpg";
        }
        String nameWithoutExtension = originalFileName.substring(0, lastDot);
        return nameWithoutExtension + "_" + suffix + ".jpg";
    }

    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    @lombok.Data
    @lombok.Builder
    public static class ThumbnailResult {
        private final String thumbnailUrl;
        private final String mediumUrl;
    }

    // Simple mock implementation of MultipartFile for internal use
    private static class MockMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public MockMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return content.length == 0;
        }

        @Override
        public long getSize() {
            return content.length;
        }

        @Override
        public byte[] getBytes() throws IOException {
            return content;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            // Implementation not needed for this use case
        }
    }
}
