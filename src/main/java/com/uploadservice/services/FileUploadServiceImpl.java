package com.uploadservice.services;


import com.uploadservice.DTO.FileDTO;
import com.uploadservice.entities.FileEntity;
import com.uploadservice.repository.FileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final FileRepository fileRepository;

    // Set of allowed file types
    private final Set<String> allowedFileTypes = new HashSet<>(
            Arrays.asList(
                    // Image MIME types
                    "image/jpeg", "image/png", "image/gif", "image/bmp", "image/tiff",
                    // Document MIME types
                    "application/pdf", "application/msword",
                    "text/plain", "text/csv", "application/rtf"
            )
    );
    private final Path rootLocation = Paths.get("uploadedFiles");

    @Autowired
    public FileUploadServiceImpl(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }
    }

    @Override
    public Mono<FileDTO> uploadFile(MultipartFile file) {
        return Mono.fromCallable(() -> {
            if (!allowedFileTypes.contains(file.getContentType())) {
                throw new RuntimeException("File type not allowed.");
            }
            if (file.isEmpty()) {
                throw new RuntimeException("Failed to store empty file.");
            }

            UUID id = UUID.randomUUID();
            String filepath = id + "_" + file.getOriginalFilename();
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filepath));

            FileDTO fileDTO = new FileDTO(
                    id,
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getContentType(),
                    Instant.now(),
                    filepath,
                    "Uploaded",
                    null
            );

            saveToDatabase(fileDTO);
            return fileDTO;
        }).flatMap(fileDTO -> {
            if (isImage(file)) {
                return createImagePreview(fileDTO.getId(), 0.5)
                        .map(previewFileDTO -> {
                            fileDTO.setPreviewImage(previewFileDTO);
                            return fileDTO;
                        });
            } else {
                return Mono.just(fileDTO);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }


    @Override
    public Mono<FileDTO> getFileById(UUID id) {
        return Mono.fromCallable(() -> fileRepository.findById(id))
                .flatMap(optionalFileEntity -> {
                    if (optionalFileEntity.isPresent()) {
                        FileEntity fileEntity = optionalFileEntity.get();
                        FileDTO fileDTO = new FileDTO(
                                fileEntity.getId(),
                                fileEntity.getFileName(),
                                fileEntity.getFileSize(),
                                fileEntity.getFileType(),
                                fileEntity.getUploadDate(),
                                fileEntity.getFilePath(),
                                fileEntity.getStatus(),
                                null// Assuming you have a way to set preview image, if needed
                        );
                        if (fileEntity.getPreviewImage() != null) {
                            FileDTO previewDTO = getPreviewFileDTO(fileEntity);
                            fileDTO.setPreviewImage(previewDTO);
                        }
                        return Mono.just(fileDTO);
                    } else {
                        return Mono.error(new NoSuchElementException("File not found with ID: " + id));
                    }
                }).subscribeOn(Schedulers.boundedElastic());
    }

    private static FileDTO getPreviewFileDTO(FileEntity fileEntity) {
        FileEntity previewEntity = fileEntity.getPreviewImage();
        return new FileDTO(
                previewEntity.getId(),
                previewEntity.getFileName(),
                previewEntity.getFileSize(),
                previewEntity.getFileType(),
                previewEntity.getUploadDate(),
                previewEntity.getFilePath(),
                previewEntity.getStatus(),
                null // Assuming the preview image itself doesn't have a preview
        );
    }

    @Override
    public Mono<Resource> loadFileAsResource(UUID id) {
        return Mono.fromCallable(() -> {
            FileEntity file = fileRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = rootLocation.resolve(file.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<FileDTO> createImagePreview(UUID fileId, double resizePercentage) {
        return Mono.fromCallable(() -> {
            FileEntity filetoConvert = fileRepository.findById(fileId)
                    .orElseThrow(() -> new RuntimeException("File not found"));


            String originalFilename = filetoConvert.getId().toString() + "_" + filetoConvert.getFileName(); // Adjust this to retrieve the original filename
            Path originalFilePath = rootLocation.resolve(originalFilename);
            BufferedImage originalImage = ImageIO.read(originalFilePath.toFile());

            int newWidth = (int) (originalImage.getWidth() * resizePercentage);
            int newHeight = (int) (originalImage.getHeight() * resizePercentage);

            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage bufferedResizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedResizedImage.createGraphics();
            g2d.drawImage(resizedImage, 0, 0, null);
            g2d.dispose();

            String previewFilename = filetoConvert.getFileName() + "_preview.jpg";
            Path previewFilePath =  rootLocation.resolve(filetoConvert.getId() +
                    "_" +previewFilename);
            ImageIO.write(bufferedResizedImage, "jpg", previewFilePath.toFile());

            // Create a new FileEntity for the resized image
            FileEntity previewFileEntity = new FileEntity();
            previewFileEntity.setId(UUID.randomUUID()); // Generate a new UUID
            previewFileEntity.setFileName(previewFilename);
            previewFileEntity.setFilePath(previewFileEntity.getId() + "_" +previewFilename);
            previewFileEntity.setFileSize(Files.size(previewFilePath)); // Get the size of the new file
            previewFileEntity.setFileType("image/jpeg");
            previewFileEntity.setUploadDate(Instant.now());
            previewFileEntity.setStatus("Uploaded");

            // Save the new FileEntity to the database
            fileRepository.save(previewFileEntity);

            //Update the original FileEntity to reference the preview image
            filetoConvert.setPreviewImage(previewFileEntity);
            fileRepository.save(filetoConvert);

            return new FileDTO(
                    previewFileEntity.getId(),
                    previewFilename,
                    Files.size(previewFilePath),
                    "image/jpeg",
                    Instant.now(),
                    previewFileEntity.getFilePath(),
                    previewFileEntity.getStatus(),
                    null);
        }).subscribeOn(Schedulers.boundedElastic());
    }


    public void saveToDatabase(FileDTO fileDTO) {
        FileEntity fileEntity = new FileEntity();

        fileEntity.setId(fileDTO.getId());
        fileEntity.setFileName(fileDTO.getFileName());
        fileEntity.setFileSize(fileDTO.getFileSize());
        fileEntity.setFileType(fileDTO.getFileType());
        fileEntity.setUploadDate(fileDTO.getUploadDate());
        fileEntity.setFilePath(fileDTO.getFilePath());
        fileEntity.setStatus(fileDTO.getStatus());

        fileRepository.save(fileEntity);
    }

    private boolean isImage(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
}
