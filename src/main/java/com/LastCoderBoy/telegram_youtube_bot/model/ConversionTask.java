package com.LastCoderBoy.telegram_youtube_bot.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionTask {
    private String taskId;
    private Long chatId;
    private String youtubeUrl;
    private VideoMetadata metadata;
    private ConversionStatus status;
    private String downloadedFilePath;
    private String convertedFilePath;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private String errorMessage;
}
