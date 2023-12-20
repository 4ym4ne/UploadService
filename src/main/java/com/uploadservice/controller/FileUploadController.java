package com.uploadservice.controller;

import com.uploadservice.services.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(
            "/upload"
    )
    public ResponseEntity<String> handleFileUpload(@RequestParam("file") MultipartFile file) {
        String filename = fileUploadService.uploadFile(file);
        // Check if the uploaded file is an image
        if (isImage(file)) {
            // Call the method to create a low-quality preview
            String previewFilename = fileUploadService.createImagePreview(filename, 0.5); // 50% quality reduction
            return ResponseEntity.ok().body("File uploaded successfully.\n" +
                    "Original File: " + filename + ",\n" +
                    "Preview File: " + previewFilename);
        }
        return ResponseEntity.ok().body("File uploaded successfully: " + filename);
    }


    @GetMapping(
            "/get/{filename:.+}"
    )
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename, HttpServletRequest request) {
        // Load file as Resource
        Resource resource = fileUploadService.loadFileAsResource(filename);

        // Try to determine file's content type
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            // Fallback to the default content type if type could not be determined
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

}

