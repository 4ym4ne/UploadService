package com.uploadservice.services;


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
import java.util.UUID;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class FileUploadServiceImpl implements FileUploadService {

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

    public FileUploadServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }
    }

    @Override
    public Mono<String> uploadFile(MultipartFile file) {
        return Mono.fromCallable(() -> {
            try {
                if (!allowedFileTypes.contains(file.getContentType())) {
                    throw new RuntimeException("File type not allowed.");
                }
                if (file.isEmpty()) {
                    throw new RuntimeException("Failed to store empty file.");
                }
                String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
                Files.copy(file.getInputStream(), this.rootLocation.resolve(filename));
                return filename;
            } catch (IOException e) {
                throw new RuntimeException("Failed to store file.", e);
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
    public Mono<String> createImagePreview(String originalFilename, double resizePercentage) {
        return Mono.fromCallable(() -> {
            Path originalFilePath = rootLocation.resolve(originalFilename);
            BufferedImage originalImage = ImageIO.read(originalFilePath.toFile());

            // Calculate new dimensions
            int newWidth = (int) (originalImage.getWidth() * resizePercentage);
            int newHeight = (int) (originalImage.getHeight() * resizePercentage);

            // Resize the image
            Image resizedImage = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            BufferedImage bufferedResizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = bufferedResizedImage.createGraphics();
            g2d.drawImage(resizedImage, 0, 0, null);
            g2d.dispose();

            // Save the resized image as a new file
            String previewFilename = "preview_" + originalFilename;
            Path previewFilePath = rootLocation.resolve(previewFilename);
            ImageIO.write(bufferedResizedImage, "jpg", previewFilePath.toFile()); // assuming JPEG format

            return previewFilename;
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
}
