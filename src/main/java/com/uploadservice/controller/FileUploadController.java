package com.uploadservice.controller;

import com.uploadservice.DTO.FileDTO;
import com.uploadservice.services.FileUploadService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

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
    public Mono<ResponseEntity<FileDTO>> handleFileUpload(@RequestPart("file") MultipartFile file) {
        return fileUploadService.uploadFile(file)
                .flatMap(uploade -> Mono.just(ResponseEntity.ok(uploade)));
    }

    @GetMapping(
            "/download/{id}"
    )
    public Mono<ResponseEntity<Resource>> downloadFile(@PathVariable UUID id,HttpServletRequest request) {
        return fileUploadService.loadFileAsResource(id)
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


    @GetMapping("/files/{id}")
    public Mono<ResponseEntity<FileDTO>> getFileById(@PathVariable UUID id) {
        return fileUploadService.getFileById(id)
                .map(ResponseEntity::ok) // maps to ResponseEntity.ok(fileDTO)
                .onErrorResume(NoSuchElementException.class,
                        e -> Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()
                        ));
    }

}

