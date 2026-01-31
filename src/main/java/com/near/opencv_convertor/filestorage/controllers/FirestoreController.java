package com.near.opencv_convertor.filestorage.controllers;

import com.near.opencv_convertor.filestorage.dto.ImageSaveResponse;
import com.near.opencv_convertor.filestorage.services.ImageService;
import com.near.opencv_convertor.firebase.dto.FirebaseUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/firestore")
@RequiredArgsConstructor
public class FirestoreController {

    private final ImageService imageService;

    @PostMapping("/save")
    public ResponseEntity<ImageSaveResponse> saveImage(
            @AuthenticationPrincipal FirebaseUserPrincipal user,
            @RequestParam MultipartFile file,
            @RequestParam String formatId
    ) {
        return ResponseEntity.ok(
                imageService.saveImage(
                        file,
                        user.uid(),
                        formatId
                )
        );
    }

    @GetMapping("/load")
    public ResponseEntity<?> getUserImages(@AuthenticationPrincipal FirebaseUserPrincipal user)
            throws ExecutionException, InterruptedException {

        return ResponseEntity.ok(Map.of("images", imageService.getUserImages(user.uid())));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteImage(
            @AuthenticationPrincipal FirebaseUserPrincipal user,
            @RequestParam String id
    ) throws ExecutionException, InterruptedException {

        imageService.deleteImage(user.uid(), id);

        return ResponseEntity.ok(
                Map.of("message", "Image deleted successfully")
        );
    }
}


