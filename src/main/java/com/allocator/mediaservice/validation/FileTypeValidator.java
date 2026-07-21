package com.allocator.mediaservice.validation;

import com.allocator.mediaservice.model.MediaFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class FileTypeValidator {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_DOCUMENT_EXTENSIONS = Set.of("pdf", "docx", "xlsx");
    private static final Set<String> ALLOWED_AUDIO_EXTENSIONS = Set.of("mp3", "wav");

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    public boolean isValidFileType(MultipartFile file, MediaFile.MediaType mediaType) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return false;
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();

        switch (mediaType) {
            case IMAGE:
                return ALLOWED_IMAGE_EXTENSIONS.contains(fileExtension);
            case DOCUMENT:
                return ALLOWED_DOCUMENT_EXTENSIONS.contains(fileExtension);
            case AUDIO:
                return ALLOWED_AUDIO_EXTENSIONS.contains(fileExtension);
            case VIDEO:
                // For now, allow all video types - can be restricted later
                return fileExtension.matches("(mp4|avi|mov|wmv|flv|webm|mkv)");
            default:
                return false;
        }
    }

    public boolean isValidFileSize(MultipartFile file) {
        return file.getSize() <= MAX_FILE_SIZE;
    }

    public MediaFile.MediaType detectMediaType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return null;
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        String contentType = file.getContentType();

        // First try by file extension
        if (ALLOWED_IMAGE_EXTENSIONS.contains(fileExtension)) {
            return MediaFile.MediaType.IMAGE;
        } else if (ALLOWED_DOCUMENT_EXTENSIONS.contains(fileExtension)) {
            return MediaFile.MediaType.DOCUMENT;
        } else if (ALLOWED_AUDIO_EXTENSIONS.contains(fileExtension)) {
            return MediaFile.MediaType.AUDIO;
        } else if (fileExtension.matches("(mp4|avi|mov|wmv|flv|webm|mkv)")) {
            return MediaFile.MediaType.VIDEO;
        }

        // Fallback to content type detection
        if (contentType != null) {
            if (contentType.startsWith("image/")) {
                return MediaFile.MediaType.IMAGE;
            } else if (contentType.startsWith("audio/")) {
                return MediaFile.MediaType.AUDIO;
            } else if (contentType.startsWith("video/")) {
                return MediaFile.MediaType.VIDEO;
            } else if (contentType.equals("application/pdf") || 
                      contentType.contains("document") || 
                      contentType.contains("sheet")) {
                return MediaFile.MediaType.DOCUMENT;
            }
        }

        return null;
    }

    public List<String> getAllowedExtensions(MediaFile.MediaType mediaType) {
        switch (mediaType) {
            case IMAGE:
                return List.copyOf(ALLOWED_IMAGE_EXTENSIONS);
            case DOCUMENT:
                return List.copyOf(ALLOWED_DOCUMENT_EXTENSIONS);
            case AUDIO:
                return List.copyOf(ALLOWED_AUDIO_EXTENSIONS);
            case VIDEO:
                return Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "webm", "mkv");
            default:
                return List.of();
        }
    }

    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    public String getValidationErrorMessage(MultipartFile file, MediaFile.MediaType mediaType) {
        List<String> errors = new java.util.ArrayList<>();

        if (!isValidFileSize(file)) {
            errors.add(String.format("File size %d bytes exceeds maximum allowed size of %d bytes", 
                    file.getSize(), MAX_FILE_SIZE));
        }

        if (!isValidFileType(file, mediaType)) {
            String extension = getFileExtension(file.getOriginalFilename());
            List<String> allowed = getAllowedExtensions(mediaType);
            errors.add(String.format("File extension '.%s' is not allowed for media type %s. Allowed extensions: %s",
                    extension, mediaType, String.join(", ", allowed)));
        }

        return String.join("; ", errors);
    }
}
