package com.LastCoderBoy.telegram_youtube_bot.service.storage;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class FileCleanupScheduler {

    private final FileStorageService fileStorageService;

    @Scheduled(fixedRateString = "${file.storage.cleanup-delay-minutes}000", initialDelay = 60000)
    public void scheduleFileCleanup() {
        log.info("Running scheduled file cleanup.. .");
        fileStorageService.cleanupOldFiles();
    }
}
