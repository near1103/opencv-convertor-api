package com.near.opencv_convertor.filestorage.services;

import com.near.opencv_convertor.filestorage.interfaces.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadRoot;

    public LocalFileStorageService(
            @Value("${storage.upload-dir:uploads}") String uploadDir
    ) {
        this.uploadRoot = Paths.get(uploadDir);
    }

    @Override
    public String save(MultipartFile file, String userId, String extension) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        try {
            Path userDir = uploadRoot.resolve(userId);
            Files.createDirectories(userDir);

            String filename = generateFilename(extension);
            Path target = userDir.resolve(filename);

            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return userId + "/" + filename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path filePath = uploadRoot.resolve(relativePath);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + relativePath, e);
        }
    }

    @Override
    public StoredFile load(String relativePath) {
        try {
            Path filePath = uploadRoot.resolve(relativePath).normalize();
            if (!filePath.startsWith(uploadRoot)) {
                throw new RuntimeException("Invalid path");
            }

            byte[] bytes = Files.readAllBytes(filePath);

            String contentType = Files.probeContentType(filePath);
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            return new StoredFile(bytes, contentType);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load file: " + relativePath, e);
        }
    }

    private String generateFilename(String extension) {
        return "image_" + UUID.randomUUID() + "." + extension;
    }
}
