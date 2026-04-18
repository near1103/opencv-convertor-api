package com.near.opencv_convertor.projects.controller;

import com.near.opencv_convertor.projects.dto.ProjectDetailsDto;
import com.near.opencv_convertor.projects.dto.ProjectOperationDto;
import com.near.opencv_convertor.projects.dto.ProjectSaveResponseDto;
import com.near.opencv_convertor.projects.dto.ProjectSummaryDto;
import com.near.opencv_convertor.projects.service.ProjectsService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/projects")
public class ProjectsController {

    private final ProjectsService projectsService;

    public ProjectsController(ProjectsService projectsService) {
        this.projectsService = projectsService;
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectSaveResponseDto> saveProject(
            @RequestPart("originalImage") MultipartFile originalImage,
            @RequestPart("resultImage") MultipartFile resultImage,
            @RequestPart("projectName") String projectName,
            @RequestPart("sourceFormatId") String sourceFormatId,
            @RequestPart("resultFormatId") String resultFormatId,
            @RequestPart("operationsJson") String operationsJson,
            @RequestPart(value = "projectId", required = false) String projectId
    ) throws Exception {
        String uid = getCurrentUserId();

        return ResponseEntity.ok(
                projectsService.saveProject(
                        uid,
                        originalImage,
                        resultImage,
                        projectName,
                        sourceFormatId,
                        resultFormatId,
                        operationsJson,
                        projectId
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<ProjectSummaryDto>> loadProjects() throws Exception {
        String uid = getCurrentUserId();
        return ResponseEntity.ok(projectsService.loadProjects(uid));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDetailsDto> loadProject(
            @PathVariable String projectId
    ) throws Exception {
        String uid = getCurrentUserId();
        return ResponseEntity.ok(projectsService.loadProjectDetails(uid, projectId));
    }

    @GetMapping("/{projectId}/operations")
    public ResponseEntity<List<ProjectOperationDto>> loadOperations(
            @PathVariable String projectId
    ) throws Exception {
        String uid = getCurrentUserId();
        return ResponseEntity.ok(projectsService.loadOperations(uid, projectId));
    }

    @GetMapping("/{projectId}/image")
    public ResponseEntity<Resource> loadProjectImage(
            @PathVariable String projectId
    ) throws Exception {
        String uid = getCurrentUserId();

        Path filePath = projectsService.getProjectResultImagePath(uid, projectId);
        FileSystemResource resource = new FileSystemResource(filePath.toFile());

        String filename = filePath.getFileName().toString().toLowerCase();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (filename.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (filename.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        } else if (filename.endsWith(".bmp")) {
            mediaType = MediaType.parseMediaType("image/bmp");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filePath.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Map<String, String>> deleteProject(
            @PathVariable String projectId
    ) throws Exception {
        String uid = getCurrentUserId();
        projectsService.deleteProject(uid, projectId);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthenticated request");
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            throw new RuntimeException("Authentication principal is null");
        }

        try {
            Method getUidMethod = principal.getClass().getMethod("getUid");
            Object value = getUidMethod.invoke(principal);
            if (value instanceof String uid && !uid.isBlank()) {
                return uid;
            }
        } catch (Exception ignored) {
        }

        String principalString = principal.toString();
        Matcher matcher = Pattern.compile("uid=([^,\\]]+)").matcher(principalString);
        if (matcher.find()) {
            return matcher.group(1);
        }

        String name = authentication.getName();
        if (name != null && !name.isBlank() && !"anonymousUser".equals(name)) {
            return name;
        }

        throw new RuntimeException("Unable to resolve current user uid");
    }

    @GetMapping("/{projectId}/base-image")
    public ResponseEntity<Resource> loadProjectBaseImage(
            @PathVariable String projectId
    ) throws Exception {
        String uid = getCurrentUserId();

        Path filePath = projectsService.getProjectBaseImagePath(uid, projectId);
        FileSystemResource resource = new FileSystemResource(filePath.toFile());

        String filename = filePath.getFileName().toString().toLowerCase();
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        if (filename.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            mediaType = MediaType.IMAGE_JPEG;
        } else if (filename.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        } else if (filename.endsWith(".bmp")) {
            mediaType = MediaType.parseMediaType("image/bmp");
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + filePath.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}