package com.LastCoderBoy.telegram_youtube_bot.service.audio;

import java.nio.file.Path;

public interface AudioConversionService {

    /**
     * Convert audio file to MP3 format
     * @param inputPath Input audio file
     * @param outputPath Output MP3 file path
     * @return Path to converted MP3 file
     */
    Path convertToMp3(Path inputPath, Path outputPath);
}
