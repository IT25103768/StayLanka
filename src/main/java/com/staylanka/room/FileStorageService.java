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
import java.util.UUID;

@Service
public class FileStorageService {
    private final StayLankaProperties properties;

    public FileStorageService(StayLankaProperties properties) {
        this.properties = properties;
    }

    public StoredFile storeRoomImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Choose an image to upload.");
        }
        if (file.getSize() > properties.uploads().maxBytes()) {
            throw new BusinessRuleException("Room image exceeds the 2 MB size limit.");
        }

        DetectedImage detected = detectImage(file);
        String storedName = UUID.randomUUID() + detected.extension();
        Path roomDirectory = roomDirectory();
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

        return new StoredFile(safeOriginalName(file.getOriginalFilename(), storedName),
                detected.contentType(), "rooms/" + storedName);
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        Path root = Path.of(properties.uploads().directory()).toAbsolutePath().normalize();
        Path target = root.resolve(relativePath).normalize();
        if (!target.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
            // Best-effort cleanup only; the database transaction result must not be changed.
        }
    }

    private DetectedImage detectImage(MultipartFile file) {
        byte[] header;
        try (InputStream input = file.getInputStream()) {
            header = input.readNBytes(12);
        } catch (IOException ex) {
            throw new BusinessRuleException("The room image could not be read.");
        }
        int read = header.length;

        if (read >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
            return new DetectedImage("image/jpeg", ".jpg");
        }
        if (read >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A) {
            return new DetectedImage("image/png", ".png");
        }
        if (read >= 12
                && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
            return new DetectedImage("image/webp", ".webp");
        }
        throw new BusinessRuleException("Only valid JPEG, PNG, and WebP room images are allowed.");
    }

    private Path roomDirectory() {
        return Path.of(properties.uploads().directory()).toAbsolutePath().normalize().resolve("rooms");
    }

    private String safeOriginalName(String originalName, String fallback) {
        if (originalName == null || originalName.isBlank()) {
            return fallback;
        }
        String safe = originalName.replace('\\', '/');
        int slash = safe.lastIndexOf('/');
        if (slash >= 0) {
            safe = safe.substring(slash + 1);
        }
        safe = safe.replaceAll("[\\p{Cntrl}]", "").trim();
        if (safe.isBlank()) {
            return fallback;
        }
        return safe.length() <= 255 ? safe : safe.substring(safe.length() - 255);
    }

    private record DetectedImage(String contentType, String extension) {
    }

    public record StoredFile(String originalName, String contentType, String relativePath) {
    }
}
