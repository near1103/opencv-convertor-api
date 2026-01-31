package com.near.opencv_convertor.firebase.services;

import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class FirestoreService {

    private Firestore db() {
        return FirestoreClient.getFirestore();
    }

    public void createUserDocument(String uid, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);

        db().collection("users")
                .document(uid)
                .set(userData);
    }

    public String addImage(String userId, String storageKey, String formatId) {
        String imageId = db().collection("users")
                .document(userId)
                .collection("images")
                .document()
                .getId();

        Map<String, Object> imageDoc = new HashMap<>();
        imageDoc.put("storageKey", storageKey);
        imageDoc.put("formatId", formatId);
        imageDoc.put("createdAt", FieldValue.serverTimestamp());

        db().collection("users")
                .document(userId)
                .collection("images")
                .document(imageId)
                .set(imageDoc);

        return imageId;
    }

    public List<Map<String, Object>> listImages(String userId) throws ExecutionException, InterruptedException {
        QuerySnapshot snap = db().collection("users")
                .document(userId)
                .collection("images")
                .get()
                .get();

        return snap.getDocuments().stream()
                .map(doc -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", doc.getId());
                    m.put("storageKey", doc.getString("storageKey"));
                    m.put("formatId", doc.getString("formatId"));
                    return m;
                })
                .toList();
    }

    public String getStorageKeyOrNull(String userId, String imageId) throws ExecutionException, InterruptedException {
        DocumentSnapshot doc = db().collection("users")
                .document(userId)
                .collection("images")
                .document(imageId)
                .get()
                .get();

        if (!doc.exists()) return null;
        return doc.getString("storageKey");
    }

    public boolean deleteImageMeta(String userId, String imageId) {
        try {
            db().collection("users")
                    .document(userId)
                    .collection("images")
                    .document(imageId)
                    .delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}