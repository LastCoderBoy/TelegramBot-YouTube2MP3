package com.LastCoderBoy.telegram_youtube_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class TelegramYoutubeBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(TelegramYoutubeBotApplication.class, args);
	}

}
