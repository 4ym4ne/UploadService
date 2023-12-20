package com.uploadservice.services;

import org.apache.commons.imaging.ImageFormats;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
    public String uploadFile(MultipartFile file) {
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
    }

    @Override
    public Resource loadFileAsResource(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read the file!");
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException("Error: " + ex.getMessage());
        }
    }

    @Override
    public String createImagePreview(String originalFilename, double resizePercentage) {
        Path originalFilePath = rootLocation.resolve(originalFilename);

        try {
            BufferedImage originalImage = Imaging.getBufferedImage(originalFilePath.toFile());
            BufferedImage resizedImage = resizeImageByPercentage(originalImage, resizePercentage);

            // Create a low-quality JPEG image
            File previewFile = new File(rootLocation.toFile(), "preview_" + originalFilename);
            ImageIO.write(resizedImage, "jpg", previewFile); // Save as JPEG

            return previewFile.getName();
        } catch (IOException | ImageReadException e) {
            throw new RuntimeException("Failed to create image preview", e);
        }
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
