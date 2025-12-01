package com.LastCoderBoy.telegram_youtube_bot.service.youtube;

import com.LastCoderBoy.telegram_youtube_bot.exception.DownloadException;
import com.LastCoderBoy.telegram_youtube_bot.model.VideoMetadata;
import com.LastCoderBoy.telegram_youtube_bot.util.CommandExecutor;
import com.LastCoderBoy.telegram_youtube_bot.util.FileValidator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class YtDlpServiceImpl implements YouTubeDownloadService {

    @Value("${youtube.download.yt-dlp-path}")
    private String ytDlpPath;

    private final CommandExecutor commandExecutor;
    private final ObjectMapper objectMapper;

    public YtDlpServiceImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Path downloadAudio(String url, Path outputPath) {
        log.info("Starting audio download from: {}", url);

        try {
            // Ensure parent directory exists
            Files.createDirectories(outputPath.getParent());

            // Build yt-dlp command
            List<String> command = new ArrayList<>();
            command.add(ytDlpPath);
            command.add("-f");
            command.add("bestaudio[ext=m4a]/bestaudio");  // Prefer m4a, fallback to best audio
            command.add("-o");
            command.add(outputPath.toString());
            command.add("--no-playlist");  // Don't download playlists
            command.add("--no-warnings");
            command.add(url);

            log.debug("Executing command: {}", String.join(" ", command));

            CommandExecutor.ProcessResult result = commandExecutor.execute(
                    command.toArray(new String[0])
            );

            if (!result.isSuccess()) {
                throw new DownloadException("yt-dlp failed with exit code: " + result.exitCode() +
                        "\nOutput: " + result.output());
            }

            // yt-dlp might add extension, find the actual file
            Path actualFile = findDownloadedFile(outputPath);

            if (actualFile == null || !Files.exists(actualFile)) {
                throw new DownloadException("Downloaded file not found at: " + outputPath);
            }

            log.info("Audio downloaded successfully: {} ({})",
                    actualFile.getFileName(),
                    FileValidator.getFileSizeReadable(Files.size(actualFile)));

            return actualFile;

        } catch (IOException | InterruptedException e) {
            log.error("Failed to download audio from: {}", url, e);
            throw new DownloadException("Failed to download audio: " + e.getMessage(), e);
        }
    }

    @Override
    public VideoMetadata getVideoMetadata(String url) {
        log.info("Fetching metadata for: {}", url);

        try {
            List<String> command = new ArrayList<>();
            command.add(ytDlpPath);
            command.add("--dump-json");
            command.add("--no-playlist");
            command.add(url);

            CommandExecutor.ProcessResult result = commandExecutor.execute(
                    command.toArray(new String[0])
            );

            if (!result.isSuccess()) {
                throw new DownloadException("Failed to fetch metadata. Exit code: " + result.exitCode());
            }

            JsonNode jsonNode = objectMapper.readTree(result.output());

            VideoMetadata metadata = VideoMetadata.builder()
                    .videoId(jsonNode.has("id") ? jsonNode.get("id").asText() : null)
                    .title(jsonNode.has("title") ? jsonNode. get("title").asText() : "Unknown")
                    .duration(jsonNode.has("duration") ? jsonNode.get("duration").asLong() : 0L)
                    .uploader(jsonNode.has("uploader") ? jsonNode.get("uploader").asText() : "Unknown")
                    .thumbnail(jsonNode.has("thumbnail") ? jsonNode.get("thumbnail").asText() : null)
                    .url(url)
                    .build();

            log.info("Metadata fetched: {}", metadata. getTitle());
            return metadata;

        } catch (IOException | InterruptedException e) {
            log.error("Failed to fetch metadata for: {}", url, e);
            throw new DownloadException("Failed to fetch video metadata: " + e.getMessage(), e);
        }
    }

    private Path findDownloadedFile(Path basePath) {
        try {
            // Check if exact file exists
            if (Files.exists(basePath)) {
                return basePath;
            }

            // yt-dlp might have added extension, check common ones
            String[] extensions = {".m4a", ".webm", ".opus", ".mp3"};
            for (String ext : extensions) {
                Path fileWithExt = Path.of(basePath.toString() + ext);
                if (Files.exists(fileWithExt)) {
                    return fileWithExt;
                }
            }

            // Check parent directory for files with similar name
            Path parent = basePath.getParent();
            String fileName = basePath.getFileName().toString();

            if (parent != null && Files.exists(parent)) {
                return Files.list(parent)
                        .filter(p -> p.getFileName().toString(). startsWith(fileName))
                        .findFirst()
                        .orElse(null);
            }

        } catch (IOException e) {
            log.error("Error finding downloaded file", e);
        }
        return null;
    }
}
