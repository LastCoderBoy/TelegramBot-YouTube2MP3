package com.LastCoderBoy.telegram_youtube_bot.config;


import com.LastCoderBoy.telegram_youtube_bot.bot.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
public class BotInitializer {

    private final TelegramBotService bot;

    public BotInitializer(TelegramBotService bot) {
        this.bot = bot;
    }

    @EventListener({ContextRefreshedEvent.class})
    public void init() throws TelegramApiException {
        log.info("Initializing Telegram Bot...");
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            telegramBotsApi.registerBot(bot);
            log.info("Telegram Bot registered successfully: @{}", bot.getBotUsername());
        } catch (TelegramApiException e) {
            log.error("Failed to register bot: {}", e.getMessage());
            throw e;
        }
    }
}
