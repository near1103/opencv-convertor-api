package com.near.opencv_convertor.filestorage.controllers;

import com.near.opencv_convertor.filestorage.interfaces.FileStorageService;
import com.near.opencv_convertor.firebase.dto.FirebaseUserPrincipal;
import com.near.opencv_convertor.firebase.services.FirestoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageContentController {

    private final FirestoreService firestoreService;
    private final FileStorageService fileStorageService;

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> getImage(
            @AuthenticationPrincipal FirebaseUserPrincipal user,
            @PathVariable("id") String id
    ) throws ExecutionException, InterruptedException {

        String storageKey = firestoreService.getStorageKeyOrNull(user.uid(), id);
        if (storageKey == null) {
            return ResponseEntity.status(404).build();
        }

        var stored = fileStorageService.load(storageKey);

        return ResponseEntity.ok()
                .header("Content-Type", stored.contentType())
                .header("Cache-Control", "private, max-age=3600")
                .body(stored.bytes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(
            @AuthenticationPrincipal FirebaseUserPrincipal user,
            @PathVariable("id") String id
    ) throws ExecutionException, InterruptedException {

        String storageKey = firestoreService.getStorageKeyOrNull(user.uid(), id);
        if (storageKey == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Not found"));
        }

        fileStorageService.delete(storageKey);
        firestoreService.deleteImageMeta(user.uid(), id);

        return ResponseEntity.ok(Map.of("message", "Image deleted successfully"));
    }
}