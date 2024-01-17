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
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    public Mono<Resource> loadFileAsResource(String filename) {
        return Mono.fromCallable(() -> {
            Path filePath = rootLocation.resolve(filename).normalize();
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
            previewFileEntity.setFilePath(previewFilePath.toString());
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
                    filetoConvert.getFileType(),
                    Instant.now(),
                    filetoConvert.getId() +"_"+ filetoConvert.getFileName() + "_preview.jpg",
                    filetoConvert.getStatus(),
                    null);
        }).subscribeOn(Schedulers.boundedElastic());
    }


    private BufferedImage resizeImageByPercentage(BufferedImage originalImage, double resizePercentage) {
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();

        int targetWidth = (int) (width * resizePercentage);
        int targetHeight = (int) (height * resizePercentage);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics2D.dispose();

        return resizedImage;
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
