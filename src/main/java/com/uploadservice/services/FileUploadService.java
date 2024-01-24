package com.uploadservice.services;
import com.uploadservice.DTO.FileDTO;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;

import java.util.UUID;


public interface FileUploadService {
    Mono<FileDTO> uploadFile(MultipartFile file, UUID messageId);

    Mono<FileDTO> getFileById(UUID id);

    Mono<Resource> loadFileAsResource(UUID id);

    Mono<FileDTO> createImagePreview(UUID fileId, double resizePercentage);
}