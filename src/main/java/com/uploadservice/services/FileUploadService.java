package com.uploadservice.services;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;



public interface FileUploadService {
    String uploadFile(MultipartFile file);
    Resource loadFileAsResource(String filename);
    String createImagePreview(String originalFilename, double resizePercentage);
}