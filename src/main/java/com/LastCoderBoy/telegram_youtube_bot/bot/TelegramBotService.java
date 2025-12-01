package com.LastCoderBoy.telegram_youtube_bot.bot;


import com.LastCoderBoy.telegram_youtube_bot.config.BotProperties;
import com.LastCoderBoy.telegram_youtube_bot.util.YouTubeUrlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Objects;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${specialUsername}")
    private String specialUsername;

    private final BotProperties botProperties;
    private final YouTubeUrlValidator urlValidator;

    public TelegramBotService(BotProperties botProperties, YouTubeUrlValidator urlValidator) {
        super(botProperties.getToken());
        this.botProperties = botProperties;
        this.urlValidator = urlValidator;
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
                            "Version: 1.0. 0\n\n" +
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
        String videoId = urlValidator.extractVideoId(url);

        sendMessage(chatId,
                "✅ Valid YouTube URL detected!\n\n" +
                        "🎬 Video ID: " + videoId + "\n\n" +
                        "⏳ Processing... (This feature is coming soon!)");

        // TODO: We'll implement the actual conversion logic next
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
}
