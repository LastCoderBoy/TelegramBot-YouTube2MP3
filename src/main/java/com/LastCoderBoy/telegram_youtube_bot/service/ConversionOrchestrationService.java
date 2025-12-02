package com.LastCoderBoy.telegram_youtube_bot.service;

import com.LastCoderBoy.telegram_youtube_bot.exception.ConversionException;
import com.LastCoderBoy.telegram_youtube_bot.exception.DownloadException;
import com.LastCoderBoy.telegram_youtube_bot.exception.FileSizeExceededException;
import com.LastCoderBoy.telegram_youtube_bot.model.ConversionStatus;
import com.LastCoderBoy.telegram_youtube_bot.model.ConversionTask;
import com.LastCoderBoy.telegram_youtube_bot.model.VideoMetadata;
import com.LastCoderBoy.telegram_youtube_bot.service.audio.AudioConversionService;
import com.LastCoderBoy.telegram_youtube_bot.service.storage.FileStorageService;
import com.LastCoderBoy.telegram_youtube_bot.service.youtube.YouTubeDownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class ConversionOrchestrationService {

    @Value("${telegram.max-file-size}")
    private long maxFileSize;

    private final YouTubeDownloadService youTubeDownloadService;
    private final AudioConversionService audioConversionService;
    private final FileStorageService fileStorageService;


    /**
     * Get video metadata without downloading
     */
    public VideoMetadata getVideoInfo(String youtubeUrl) {
        log.info("Fetching video info for: {}", youtubeUrl);
        return youTubeDownloadService.getVideoMetadata(youtubeUrl);
    }

    /**
     * Process YouTube URL: download → convert → return file path
     * This runs asynchronously
     */
    @Async
    public CompletableFuture<ConversionTask> processVideo(String youtubeUrl, Long chatId) {
        String taskId = UUID.randomUUID().toString();

        ConversionTask task = ConversionTask.builder()
                .taskId(taskId)
                .chatId(chatId)
                .youtubeUrl(youtubeUrl)
                .status(ConversionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        try {
            // Step 1: Get video metadata
            log.info("[{}] Fetching video metadata", taskId);
            task.setStatus(ConversionStatus.DOWNLOADING);
            VideoMetadata metadata = youTubeDownloadService.getVideoMetadata(youtubeUrl);
            task.setMetadata(metadata);

            log.info("[{}] Video: {} by {}", taskId, metadata.getTitle(), metadata.getUploader());

            // Step 2: Download audio
            log.info("[{}] Downloading audio", taskId);
            String sanitizedTitle = fileStorageService.sanitizeFileName(metadata.getTitle());
            Path downloadPath = fileStorageService.getDownloadPath(sanitizedTitle);

            Path downloadedFile = youTubeDownloadService.downloadAudio(youtubeUrl, downloadPath);
            task.setDownloadedFilePath(downloadedFile.toString());

            log.info("[{}] Download completed: {}", taskId, downloadedFile.getFileName());

            // Step 3: Convert to MP3
            log.info("[{}] Converting to MP3", taskId);
            task.setStatus(ConversionStatus.CONVERTING);

            Path mp3Path = fileStorageService.getConvertedPath(sanitizedTitle + ".mp3");
            Path convertedFile = audioConversionService.convertToMp3(downloadedFile, mp3Path);
            task.setConvertedFilePath(convertedFile.toString());

            log.info("[{}] Conversion completed: {}", taskId, convertedFile.getFileName());

            // Step 4: Check file size
            long fileSize = fileStorageService.getFileSize(convertedFile);
            log.info("[{}] MP3 file size: {}", taskId, fileStorageService.getFileSizeReadable(fileSize));

            if (fileSize > maxFileSize) {
                throw new FileSizeExceededException(
                        String.format("File size (%s) exceeds Telegram limit (%s)",
                                fileStorageService.getFileSizeReadable(fileSize),
                                fileStorageService.getFileSizeReadable(maxFileSize))
                );
            }

            // Step 5: Cleanup downloaded file (keep only MP3)
            fileStorageService.deleteFile(downloadedFile);

            // Mark as completed
            task.setStatus(ConversionStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());

            log.info("[{}] Processing completed successfully", taskId);
            return CompletableFuture.completedFuture(task);

        } catch (DownloadException e) {
            log.error("[{}] Download failed: {}", taskId, e.getMessage());
            task.setStatus(ConversionStatus.FAILED);
            task.setErrorMessage("Download failed: " + e.getMessage());
            return CompletableFuture.completedFuture(task);

        } catch (ConversionException e) {
            log. error("[{}] Conversion failed: {}", taskId, e.getMessage());
            task.setStatus(ConversionStatus.FAILED);
            task.setErrorMessage("Conversion failed: " + e. getMessage());
            return CompletableFuture.completedFuture(task);

        } catch (FileSizeExceededException e) {
            log.error("[{}] File size exceeded: {}", taskId, e.getMessage());
            task.setStatus(ConversionStatus.FAILED);
            task.setErrorMessage(e.getMessage());

            // Cleanup files
            if (task.getDownloadedFilePath() != null) {
                fileStorageService.deleteFile(Path.of(task.getDownloadedFilePath()));
            }
            if (task.getConvertedFilePath() != null) {
                fileStorageService.deleteFile(Path.of(task. getConvertedFilePath()));
            }

            return CompletableFuture.completedFuture(task);

        } catch (Exception e) {
            log.error("[{}] Unexpected error: {}", taskId, e.getMessage(), e);
            task.setStatus(ConversionStatus.FAILED);
            task.setErrorMessage("Unexpected error: " + e.getMessage());
            return CompletableFuture.completedFuture(task);
        }
    }

    /**
     * Cleanup task files after upload
     */
    public void cleanupTask(ConversionTask task) {
        if (task.getDownloadedFilePath() != null) {
            fileStorageService.deleteFile(Path.of(task.getDownloadedFilePath()));
        }
        if (task.getConvertedFilePath() != null) {
            fileStorageService.deleteFile(Path. of(task.getConvertedFilePath()));
        }
        log.info("[{}] Task files cleaned up", task.getTaskId());
    }
}
