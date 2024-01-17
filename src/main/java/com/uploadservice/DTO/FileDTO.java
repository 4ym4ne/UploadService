package com.uploadservice.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for {@link com.uploadservice.entities.FileEntity}
 */
@Setter
@Getter
@AllArgsConstructor
public class FileDTO implements Serializable {
    UUID id;
    String fileName;
    Long fileSize;
    String fileType;
    Instant uploadDate;
    String filePath;
    String status;
    FileDTO previewImage;

}