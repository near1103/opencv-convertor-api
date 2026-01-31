package com.near.opencv_convertor.auth.controllers;

import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.near.opencv_convertor.firebase.services.FirebaseAuthService;
import com.near.opencv_convertor.firebase.dto.FirebaseUserPrincipal;
import com.near.opencv_convertor.firebase.services.FirestoreService;
import com.near.opencv_convertor.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final FirebaseAuthService firebaseAuthService;
    private final FirestoreService firestoreService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            UserRecord userRecord = firebaseAuthService.register(request.email(), request.password());
            firestoreService.createUserDocument(userRecord.getUid(), userRecord.getEmail());

            String jwt = jwtService.generateToken(userRecord.getUid(), userRecord.getEmail());

            return ResponseEntity.ok(new LoginResponse(
                    userRecord.getUid(),
                    userRecord.getEmail(),
                    jwt
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Register failed: " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal FirebaseUserPrincipal user) {
        if (user == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Unauthorized"));
        }
        return ResponseEntity.ok(new UserMeResponse(user.uid(), user.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            String idToken = firebaseAuthService.signIn(request.email(), request.password());
            FirebaseToken decodedToken = firebaseAuthService.verify(idToken);

            String jwt = jwtService.generateToken(decodedToken.getUid(), decodedToken.getEmail());

            return ResponseEntity.ok(new LoginResponse(
                    decodedToken.getUid(),
                    decodedToken.getEmail(),
                    jwt
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Login failed"));
        }
    }

    record LoginRequest(String email, String password) {}
    record LoginResponse(String uid, String email, String token) {}
    record ErrorResponse(String message) {}
    record RegisterRequest(String email, String password) {}
    record RegisterResponse(String uid, String email, String message) {}
    record UserMeResponse(String uid, String email) {}
}
