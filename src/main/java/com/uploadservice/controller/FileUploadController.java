package com.uploadservice.controller;

import com.uploadservice.DTO.FileUploadResponseDTO;
import com.uploadservice.services.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

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
    public Mono<ResponseEntity<FileUploadResponseDTO>> handleFileUpload(@RequestPart("file") MultipartFile file) {
        return fileUploadService.uploadFile(file)
                .flatMap(filename -> {
                    // Check if the uploaded file is an image
                    if (isImage(file)) {
                        return fileUploadService.createImagePreview(filename, 0.5) // 50% quality reduction
                                .map(previewFilename -> ResponseEntity.ok(new FileUploadResponseDTO(filename, previewFilename)));
                    } else {
                        return Mono.just(ResponseEntity.ok(new FileUploadResponseDTO(filename, "Not Supported")));
                    }
                });
    }

    @GetMapping(
            "/get/{filename:.+}"
    )
    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable String filename,HttpServletRequest request) {
        return fileUploadService.loadFileAsResource(filename)
                .map(resource -> {
                    // Try to determine file's content type
                    String contentType = null;
                    try {
                        contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                    } catch (IOException ex) {
                        // Fallback to the default content type if type could not be determined
                        contentType = "application/octet-stream";
                    } // Default content type
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                            .body(resource);
                });
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

}

