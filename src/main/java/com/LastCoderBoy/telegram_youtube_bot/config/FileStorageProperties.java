package com.LastCoderBoy.telegram_youtube_bot.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {
    private String basePath;
    private String downloadPath;
    private String convertedPath;
    private int cleanupDelayMinutes = 30;

    public Path getDownloadDirectory() {
        return Paths.get(downloadPath);
    }

    public Path getConvertedDirectory() {
        return Paths.get(convertedPath);
    }
}
