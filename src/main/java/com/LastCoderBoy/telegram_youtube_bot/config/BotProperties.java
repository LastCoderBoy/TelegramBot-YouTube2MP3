package com.LastCoderBoy.telegram_youtube_bot.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
public class BotProperties {
    private String username;
    private String token;
}
