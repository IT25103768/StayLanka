package com.staylanka.room;

import com.staylanka.common.BusinessRuleException;
import com.staylanka.config.StayLankaProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final StayLankaProperties properties;

    public FileStorageService(StayLankaProperties properties) {
        this.properties = properties;
    }

    public StoredFile storeRoomImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Choose an image to upload.");
        }
        String contentType = file.getContentType();
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new BusinessRuleException("Only JPEG, PNG, and WebP room images are allowed.");
        }
        if (file.getSize() > properties.uploads().maxBytes()) {
            throw new BusinessRuleException("Room image exceeds the 2 MB size limit.");
        }
        String storedName = UUID.randomUUID() + extension;
        Path roomDirectory = Path.of(properties.uploads().directory()).toAbsolutePath().normalize().resolve("rooms");
        Path destination = roomDirectory.resolve(storedName).normalize();
        if (!destination.startsWith(roomDirectory)) {
            throw new BusinessRuleException("Invalid image path.");
        }
        try {
            Files.createDirectories(roomDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new BusinessRuleException("The room image could not be stored.");
        }
        return new StoredFile(file.getOriginalFilename() == null ? storedName : file.getOriginalFilename(),
                contentType, "rooms/" + storedName);
    }

    public record StoredFile(String originalName, String contentType, String relativePath) {
    }
}
