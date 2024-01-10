package com.uploadservice.services;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Mono;


public interface FileUploadService {
    Mono<String> uploadFile(MultipartFile file);
    Mono<Resource> loadFileAsResource(String filename);
    Mono<String> createImagePreview(String originalFilename, double resizePercentage);
}