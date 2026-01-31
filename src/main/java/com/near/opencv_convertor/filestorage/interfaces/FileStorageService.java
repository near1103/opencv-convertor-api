package com.near.opencv_convertor.filestorage.interfaces;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String save(MultipartFile file, String userId, String extension);
    void delete(String relativePath);
    StoredFile load(String relativePath);
    record StoredFile(byte[] bytes, String contentType) {}
}