package com.LastCoderBoy.telegram_youtube_bot.bot;


import com.LastCoderBoy.telegram_youtube_bot.config.BotProperties;
import com.LastCoderBoy.telegram_youtube_bot.model.ConversionStatus;
import com.LastCoderBoy.telegram_youtube_bot.model.ConversionTask;
import com.LastCoderBoy.telegram_youtube_bot.model.VideoMetadata;
import com.LastCoderBoy.telegram_youtube_bot.service.ConversionOrchestrationService;
import com.LastCoderBoy.telegram_youtube_bot.util.YouTubeUrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendAudio;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.Objects;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${specialUsername}")
    private String specialUsername;

    private final BotProperties botProperties;
    private final YouTubeUrlValidator urlValidator;
    private final ConversionOrchestrationService orchestrationService;


    public TelegramBotService(BotProperties botProperties, YouTubeUrlValidator urlValidator, ConversionOrchestrationService orchestrationService) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.urlValidator = urlValidator;
        this.orchestrationService = orchestrationService;
        log.info("TelegramBotService initialized with username: {}", botProperties.getUsername());
    }

    @Override
    public String getBotUsername() {
        return botProperties.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String userName = update.getMessage().getFrom().getFirstName();

            log.info("Received message from {}: {}", userName, messageText);

            // Handle commands
            if (messageText.startsWith("/")) {
                handleCommand(chatId, messageText, userName);
            } else if (urlValidator.isValidYouTubeUrl(messageText)) {
                handleYouTubeUrl(chatId, messageText);
            } else {
                sendMessage(chatId, "❌ Invalid YouTube URL!\n\n" +
                        "Please send a valid YouTube URL like:\n" +
                        "• https://www.youtube.com/watch?v=VIDEO_ID\n" +
                        "• https://youtu. be/VIDEO_ID");
            }
        }
    }

    private void handleCommand(Long chatId, String command, String userName) {
        userName = (Objects.equals(userName, specialUsername)) ? "Gulim !" : userName;
        switch (command.toLowerCase()) {
            case "/start" -> sendMessage(chatId,
                    "👋 Hello " + userName + "!\n\n" +
                            "Welcome to YouTube to MP3 Converter Bot! 🎵\n\n" +
                            "Simply send me a YouTube URL and I'll convert it to MP3 for you!\n\n" +
                            "Commands:\n" +
                            "/help - Show help message\n" +
                            "/about - About this bot");

            case "/help" -> sendMessage(chatId,
                    "📖 How to use:\n\n" +
                            "1. Copy a YouTube video URL\n" +
                            "2. Send it to me\n" +
                            "3. Wait for the conversion\n" +
                            "4. Download your MP3 file!\n\n" +
                            "Supported formats:\n" +
                            "• https://www.youtube.com/watch?v=VIDEO_ID\n" +
                            "• https://youtu.be/VIDEO_ID\n\n" +
                            "⚠️ Note: Files larger than 50MB cannot be sent via Telegram.");

            case "/about" -> sendMessage(chatId,
                    "ℹ️ About this bot:\n\n" +
                            "YouTube to MP3 Converter Bot\n" +
                            "Version: 1.0.0\n\n" +
                            "Built with ❤️ using:\n" +
                            "• Java & Spring Boot\n" +
                            "• yt-dlp\n" +
                            "• FFmpeg\n\n" +
                            "Developer: @just_search");

            default -> sendMessage(chatId, "Unknown command. Type /help for available commands.");
        }
    }

    private void handleYouTubeUrl(Long chatId, String url) {
        log.info("Processing YouTube URL: {}", url);

        try {
            // Step 1: Fetch video metadata
            sendMessage(chatId, "🔍 Fetching video information...");
            VideoMetadata metadata = orchestrationService.getVideoInfo(url);

            // Step 2: Show video info
            String videoInfo = String.format(
                    "✅ Video found!\n\n" +
                            "🎬 Title: %s\n" +
                            "👤 Channel: %s\n" +
                            "⏱ Duration: %s\n\n" +
                            "⏳ Starting conversion...",
                    metadata.getTitle(),
                    metadata.getUploader(),
                    formatDuration(metadata.getDuration())
            );
            sendMessage(chatId, videoInfo);

            // Step 3: Start async processing
            orchestrationService.processVideo(url, chatId)
                    .thenAccept(task -> handleConversionResult(chatId, task))
                    .exceptionally(ex -> {
                        log.error("Error processing video", ex);
                        sendMessage(chatId, "❌ An error occurred: " + ex.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            log.error("Failed to process YouTube URL: {}", url, e);
            sendMessage(chatId, "❌ Failed to fetch video information.  Please check the URL and try again.");
        }
    }


    private void handleConversionResult(Long chatId, ConversionTask task) {
        if (task.getStatus() == ConversionStatus.COMPLETED) {
            log.info("[{}] Conversion successful, uploading file", task.getTaskId());

            sendMessage(chatId, "✅ Conversion completed!  Uploading.. .");

            // Upload the MP3 file
            File mp3File = new File(task.getConvertedFilePath());
            sendAudioFile(chatId, mp3File, task.getMetadata());

            // Cleanup
            orchestrationService.cleanupTask(task);

        } else if (task.getStatus() == ConversionStatus. FAILED) {
            log.error("[{}] Conversion failed: {}", task.getTaskId(), task.getErrorMessage());
            sendMessage(chatId, "❌ Conversion failed!\n\n" +
                    "Unable to process the request for the URL: " + task.getYoutubeUrl());
        }
    }

    private void sendAudioFile(Long chatId, File audioFile, VideoMetadata metadata) {
        try {
            SendAudio sendAudio = SendAudio.builder()
                    .chatId(chatId. toString())
                    .audio(new InputFile(audioFile))
                    .title(metadata.getTitle())
                    .performer(metadata.getUploader())
                    .caption("🎵 " + metadata.getTitle())
                    .build();

            execute(sendAudio);
            log.info("Audio file sent successfully to chatId: {}", chatId);

            sendMessage(chatId, "✅ Done! Enjoy your music!  🎵");

        } catch (TelegramApiException e) {
            log. error("Failed to send audio file to chatId: {}", chatId, e);
            sendMessage(chatId, "❌ Failed to upload the audio file. It might be too large.");
        }
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage message = SendMessage. builder()
                .chatId(chatId. toString())
                .text(text)
                .build();

        try {
            execute(message);
            log.debug("Message sent to {}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}: {}", chatId, e. getMessage());
        }
    }

    private String formatDuration(Long seconds) {
        if (seconds == null || seconds == 0) {
            return "Unknown";
        }
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }
}
