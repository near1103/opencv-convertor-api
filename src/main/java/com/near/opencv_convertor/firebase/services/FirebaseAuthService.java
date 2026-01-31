package com.near.opencv_convertor.firebase.services;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.google.firebase.auth.UserRecord;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class FirebaseAuthService {

    private final FirebaseApp firebaseApp;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${firebase.api-key}")
    private String firebaseApiKey;


    private FirebaseAuth getAuth() {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    public FirebaseToken verify(String idToken) throws FirebaseAuthException {
        return getAuth().verifyIdToken(idToken);
    }

    public UserRecord register(String email, String password) throws FirebaseAuthException {
        UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password);
        return getAuth().createUser(request);
    }

    public String signIn(String email, String password) {
        if (firebaseApiKey == null || firebaseApiKey.isBlank()) {
            throw new RuntimeException("firebase.api-key is missing");
        }

        String url =
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                        + firebaseApiKey;

        Map<String, Object> body = Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", true
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.postForObject(url, body, Map.class);

            if (response == null || !response.containsKey("idToken")) {
                throw new RuntimeException("Firebase signIn returned no idToken");
            }

            return response.get("idToken").toString();

        } catch (HttpStatusCodeException ex) {
            throw new RuntimeException(
                    "Firebase signIn error: " + ex.getResponseBodyAsString(), ex
            );
        }
    }
}
