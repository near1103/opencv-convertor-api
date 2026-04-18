package com.near.opencv_convertor.projects.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.near.opencv_convertor.projects.dto.ProjectDetailsDto;
import com.near.opencv_convertor.projects.dto.ProjectOperationDto;
import com.near.opencv_convertor.projects.dto.ProjectSaveResponseDto;
import com.near.opencv_convertor.projects.dto.ProjectSummaryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProjectsService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ProjectSaveResponseDto saveProject(
            String uid,
            MultipartFile originalImage,
            MultipartFile resultImage,
            String projectName,
            String sourceFormatId,
            String resultFormatId,
            String operationsJson,
            String projectId
    ) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        String finalProjectId = (projectId == null || projectId.isBlank())
                ? UUID.randomUUID().toString()
                : projectId;

        List<ProjectOperationDto> operations = parseOperations(operationsJson);

        String baseExt = normalizeExt(sourceFormatId, originalImage.getOriginalFilename());
        String resultExt = normalizeExt(resultFormatId, resultImage.getOriginalFilename());

        String baseRelativePath = uid + "/projects/" + finalProjectId + "/base." + baseExt;
        String resultRelativePath = uid + "/projects/" + finalProjectId + "/result." + resultExt;

        saveFile(originalImage, baseRelativePath);
        saveFile(resultImage, resultRelativePath);

        long now = System.currentTimeMillis();

        DocumentReference projectRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(finalProjectId);

        DocumentSnapshot existing = projectRef.get().get();

        Long createdAt = existing.exists() && existing.contains("createdAt")
                ? existing.getLong("createdAt")
                : now;

        Map<String, Object> projectData = new HashMap<>();
        projectData.put("name", projectName);
        projectData.put("basePath", baseRelativePath);
        projectData.put("resultPath", resultRelativePath);
        projectData.put("baseFormatId", sourceFormatId);
        projectData.put("resultFormatId", resultFormatId);
        projectData.put("createdAt", createdAt);
        projectData.put("updatedAt", now);
        projectData.put("operationsCount", operations.size());
        projectData.put("previewTool", operations.isEmpty() ? null : operations.get(operations.size() - 1).getTool());

        projectRef.set(projectData).get();

        deleteOperations(projectRef);

        CollectionReference operationsRef = projectRef.collection("operations");
        for (ProjectOperationDto operation : operations) {
            Map<String, Object> opMap = new HashMap<>();
            opMap.put("order", operation.getOrder());
            opMap.put("category", operation.getCategory());
            opMap.put("tool", operation.getTool());
            opMap.put("params", operation.getParams() == null ? new HashMap<>() : operation.getParams());
            opMap.put("createdAt", now);

            operationsRef.add(opMap).get();
        }

        return new ProjectSaveResponseDto(finalProjectId, "Project saved successfully");
    }

    public List<ProjectSummaryDto> loadProjects(String uid) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("users")
                .document(uid)
                .collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get();

        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        return docs.stream().map(doc -> {
            ProjectSummaryDto dto = new ProjectSummaryDto();
            dto.setProjectId(doc.getId());
            dto.setName(doc.getString("name"));
            dto.setSourcePath(doc.getString("basePath"));
            dto.setResultPath(doc.getString("resultPath"));
            dto.setSourceFormatId(doc.getString("baseFormatId"));
            dto.setResultFormatId(doc.getString("resultFormatId"));
            dto.setCreatedAt(doc.getLong("createdAt"));
            dto.setUpdatedAt(doc.getLong("updatedAt"));
            dto.setOperationsCount(doc.getLong("operationsCount") == null ? 0 : doc.getLong("operationsCount").intValue());
            dto.setPreviewTool(doc.getString("previewTool"));
            return dto;
        }).collect(Collectors.toList());
    }

    public ProjectDetailsDto loadProjectDetails(String uid, String projectId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        DocumentReference projectRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(projectId);

        DocumentSnapshot doc = projectRef.get().get();
        if (!doc.exists()) {
            throw new RuntimeException("Project not found");
        }

        ProjectSummaryDto project = new ProjectSummaryDto();
        project.setProjectId(doc.getId());
        project.setName(doc.getString("name"));
        project.setSourcePath(doc.getString("basePath"));
        project.setResultPath(doc.getString("resultPath"));
        project.setSourceFormatId(doc.getString("baseFormatId"));
        project.setResultFormatId(doc.getString("resultFormatId"));
        project.setCreatedAt(doc.getLong("createdAt"));
        project.setUpdatedAt(doc.getLong("updatedAt"));
        project.setOperationsCount(doc.getLong("operationsCount") == null ? 0 : doc.getLong("operationsCount").intValue());
        project.setPreviewTool(doc.getString("previewTool"));

        List<ProjectOperationDto> operations = loadOperations(uid, projectId);

        ProjectDetailsDto details = new ProjectDetailsDto();
        details.setProject(project);
        details.setOperations(operations);
        return details;
    }

    public List<ProjectOperationDto> loadOperations(String uid, String projectId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(projectId)
                .collection("operations")
                .orderBy("order", Query.Direction.ASCENDING)
                .get();

        List<QueryDocumentSnapshot> docs = future.get().getDocuments();

        List<ProjectOperationDto> result = new ArrayList<>();
        for (QueryDocumentSnapshot doc : docs) {
            ProjectOperationDto dto = new ProjectOperationDto();
            dto.setOrder(doc.getLong("order") == null ? 0 : doc.getLong("order").intValue());
            dto.setCategory(doc.getString("category"));
            dto.setTool(doc.getString("tool"));

            Object paramsObject = doc.get("params");
            if (paramsObject instanceof Map<?, ?> map) {
                Map<String, Object> params = new HashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    params.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                dto.setParams(params);
            } else {
                dto.setParams(new HashMap<>());
            }

            result.add(dto);
        }

        return result;
    }

    public void deleteProject(String uid, String projectId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        DocumentReference projectRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(projectId);

        DocumentSnapshot projectDoc = projectRef.get().get();
        if (!projectDoc.exists()) {
            return;
        }

        String basePath = projectDoc.getString("basePath");
        String resultPath = projectDoc.getString("resultPath");

        deleteOperations(projectRef);
        projectRef.delete().get();

        deletePhysicalFileIfExists(basePath);
        deletePhysicalFileIfExists(resultPath);
    }

    private List<ProjectOperationDto> parseOperations(String operationsJson) throws IOException {
        if (operationsJson == null || operationsJson.isBlank()) {
            return new ArrayList<>();
        }

        return objectMapper.readValue(
                operationsJson,
                new TypeReference<List<ProjectOperationDto>>() {
                }
        );
    }

    private void deleteOperations(DocumentReference projectRef) throws Exception {
        CollectionReference operationsRef = projectRef.collection("operations");
        List<QueryDocumentSnapshot> existingOps = operationsRef.get().get().getDocuments();

        for (QueryDocumentSnapshot opDoc : existingOps) {
            opDoc.getReference().delete().get();
        }
    }

    private String normalizeExt(String formatId, String originalFilename) {
        if (formatId != null && !formatId.isBlank()) {
            return formatId.toLowerCase();
        }

        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }

        return "png";
    }

    private void saveFile(MultipartFile file, String relativePath) throws IOException {
        Path fullPath = Paths.get(uploadDir, relativePath);
        Files.createDirectories(fullPath.getParent());
        Files.write(
                fullPath,
                file.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private void deletePhysicalFileIfExists(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;

        try {
            Path fullPath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(fullPath);
        } catch (IOException ignored) {
        }
    }

    public Path getProjectResultImagePath(String uid, String projectId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        DocumentReference projectRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(projectId);

        DocumentSnapshot doc = projectRef.get().get();
        if (!doc.exists()) {
            throw new RuntimeException("Project not found");
        }

        String resultPath = doc.getString("resultPath");
        if (resultPath == null || resultPath.isBlank()) {
            throw new RuntimeException("Project result image not found");
        }

        Path fullPath = Paths.get(uploadDir, resultPath);
        if (!Files.exists(fullPath)) {
            throw new RuntimeException("Project result file does not exist");
        }

        return fullPath;
    }

    public Path getProjectBaseImagePath(String uid, String projectId) throws Exception {
        Firestore db = FirestoreClient.getFirestore();

        DocumentReference projectRef = db.collection("users")
                .document(uid)
                .collection("projects")
                .document(projectId);

        DocumentSnapshot doc = projectRef.get().get();
        if (!doc.exists()) {
            throw new RuntimeException("Project not found");
        }

        String basePath = doc.getString("basePath");
        if (basePath == null || basePath.isBlank()) {
            throw new RuntimeException("Project base image not found");
        }

        Path fullPath = Paths.get(uploadDir, basePath);
        if (!Files.exists(fullPath)) {
            throw new RuntimeException("Project base file does not exist");
        }

        return fullPath;
    }
}