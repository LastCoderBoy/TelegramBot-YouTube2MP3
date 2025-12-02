package com.LastCoderBoy.telegram_youtube_bot.service.storage;

import com.LastCoderBoy.telegram_youtube_bot.config.FileStorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java. time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileStorageProperties properties;


    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(properties.getDownloadDirectory());
            Files.createDirectories(properties.getConvertedDirectory());
            log.info("Storage directories initialized:");
            log.info("  - Downloads: {}", properties.getDownloadDirectory());
            log.info("  - Converted: {}", properties.getConvertedDirectory());
        } catch (IOException e) {
            log.error("Failed to create storage directories", e);
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }

    public Path getDownloadPath(String fileName) {
        return properties.getDownloadDirectory().resolve(sanitizeFileName(fileName));
    }

    public Path getConvertedPath(String fileName) {
        return properties.getConvertedDirectory().resolve(sanitizeFileName(fileName));
    }

    public String sanitizeFileName(String fileName) {
        // Remove invalid characters and limit length
        return fileName
                .replaceAll("[^a-zA-Z0-9.-]", "_")
                .substring(0, Math.min(fileName.length(), 200));
    }

    public boolean deleteFile(Path filePath) {
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log. debug("Deleted file: {}", filePath);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
            return false;
        }
    }

    public long getFileSize(Path filePath) {
        try {
            return Files.size(filePath);
        } catch (IOException e) {
            log.error("Failed to get file size: {}", filePath, e);
            return 0;
        }
    }

    public String getFileSizeReadable(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math. log(bytes) / Math.log(1024));
        String pre = "KMGTPE". charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public void cleanupOldFiles() {
        cleanupDirectory(properties.getDownloadDirectory());
        cleanupDirectory(properties.getConvertedDirectory());
    }

    private void cleanupDirectory(Path directory) {
        try {
            File dir = directory.toFile();
            if (!dir.exists()) return;

            File[] files = dir.listFiles();
            if (files == null) return;

            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(properties.getCleanupDelayMinutes());

            for (File file : files) {
                LocalDateTime fileTime = LocalDateTime.ofInstant(
                        Files.getLastModifiedTime(file.toPath()). toInstant(),
                        ZoneId.systemDefault()
                );

                if (fileTime.isBefore(cutoffTime)) {
                    if (file.delete()) {
                        log.info("Cleaned up old file: {}", file. getName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error during cleanup of directory: {}", directory, e);
        }
    }
}
