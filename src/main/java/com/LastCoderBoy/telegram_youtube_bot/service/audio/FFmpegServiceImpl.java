package com.LastCoderBoy.telegram_youtube_bot.service.audio;


import com.LastCoderBoy.telegram_youtube_bot.exception.ConversionException;
import com.LastCoderBoy.telegram_youtube_bot.service.storage.FileStorageService;
import com.LastCoderBoy.telegram_youtube_bot.util.CommandExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FFmpegServiceImpl implements AudioConversionService {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffmpeg.audio-bitrate}")
    private String audioBitrate;

    @Value("${ffmpeg.audio-codec}")
    private String audioCodec;

    private final CommandExecutor commandExecutor;
    private final FileStorageService fileStorageService;

    @Override
    public Path convertToMp3(Path inputPath, Path outputPath) {
        log.info("Converting {} to MP3", inputPath.getFileName());

        if (!Files.exists(inputPath)) {
            throw new ConversionException("Input file does not exist: " + inputPath);
        }

        try {
            // Ensure output directory exists
            Files.createDirectories(outputPath.getParent());

            // Build FFmpeg command
            List<String> command = new ArrayList<>();
            command.add(ffmpegPath);
            command.add("-i");
            command.add(inputPath.toString());
            command.add("-vn");  // No video
            command.add("-ar");
            command.add("44100");  // Sample rate
            command.add("-ac");
            command.add("2");  // Audio channels (stereo)
            command.add("-b:a");
            command.add(audioBitrate);
            command.add("-acodec");
            command.add(audioCodec);
            command.add("-y");  // Overwrite output file
            command.add(outputPath.toString());

            log.debug("Executing FFmpeg command: {}", String.join(" ", command));

            CommandExecutor.ProcessResult result = commandExecutor.execute(
                    command.toArray(new String[0])
            );

            if (! result.isSuccess()) {
                throw new ConversionException("FFmpeg failed with exit code: " + result. exitCode() +
                        "\nOutput: " + result.output());
            }

            if (! Files.exists(outputPath)) {
                throw new ConversionException("Output file was not created: " + outputPath);
            }

            long outputSize = Files.size(outputPath);
            log.info("Conversion completed: {} ({})",
                    outputPath.getFileName(),
                    fileStorageService.getFileSizeReadable(outputSize));

            return outputPath;

        } catch (IOException | InterruptedException e) {
            log.error("Failed to convert audio file", e);
            throw new ConversionException("Audio conversion failed: " + e.getMessage(), e);
        }
    }
}
