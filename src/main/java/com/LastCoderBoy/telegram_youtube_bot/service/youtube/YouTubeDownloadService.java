package com.LastCoderBoy.telegram_youtube_bot.service.youtube;


import com.LastCoderBoy.telegram_youtube_bot.model.VideoMetadata;

import java.nio.file.Path;

public interface YouTubeDownloadService {

    /**
     * Download audio from YouTube video
     * @param url YouTube video URL
     * @param outputPath Where to save the downloaded file
     * @return Path to the downloaded file
     */
    Path downloadAudio(String url, Path outputPath);

    /**
     * Get video metadata without downloading
     * @param url YouTube video URL
     * @return Video metadata
     */
    VideoMetadata getVideoMetadata(String url);
}
