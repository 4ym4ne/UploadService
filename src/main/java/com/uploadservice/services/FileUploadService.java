package com.uploadservice.services;
import com.uploadservice.DTO.FileDTO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface FileUploadService {
    Mono<FileDTO> uploadFile(MultipartFile file);
    Mono<Resource> loadFileAsResource(String filename);
    Mono<FileDTO> createImagePreview(UUID fileId, double resizePercentage);
}