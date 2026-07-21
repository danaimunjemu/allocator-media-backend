package com.allocator.mediaservice.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {

    String uploadFile(MultipartFile file, String folder);

    InputStream downloadFile(String objectName);

    void deleteFile(String objectName);

    String generatePresignedUrl(String objectName, int expiryMinutes);

    boolean fileExists(String objectName);
}
