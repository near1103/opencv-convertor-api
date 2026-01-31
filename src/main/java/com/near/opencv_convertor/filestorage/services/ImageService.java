package com.near.opencv_convertor.filestorage.services;

import com.near.opencv_convertor.filestorage.dto.ImageSaveResponse;
import com.near.opencv_convertor.filestorage.interfaces.FileStorageService;
import com.near.opencv_convertor.firebase.services.FirestoreService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@AllArgsConstructor
public class ImageService {

    private final FileStorageService fileStorageService;
    private final FirestoreService firestoreService;

    public ImageSaveResponse saveImage(MultipartFile file, String userId, String formatId) {
        String storageKey = fileStorageService.save(file, userId, formatId);
        String imageId = firestoreService.addImage(userId, storageKey, formatId);
        return new ImageSaveResponse("Image saved successfully", imageId, formatId);
    }

    public List<Map<String, Object>> getUserImages(String userId) throws ExecutionException, InterruptedException {
        return firestoreService.listImages(userId);
    }

    public void deleteImage(String userId, String imageId) throws ExecutionException, InterruptedException {
        String storageKey = firestoreService.getStorageKeyOrNull(userId, imageId);

        if (storageKey == null || storageKey.isBlank()) {
            throw new RuntimeException("Image not found for user: " + imageId);
        }

        fileStorageService.delete(storageKey);

        boolean deleted = firestoreService.deleteImageMeta(userId, imageId);
        if (!deleted) {
            throw new RuntimeException("Failed to delete image meta: " + imageId);
        }
    }

}