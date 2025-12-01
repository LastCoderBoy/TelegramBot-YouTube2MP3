package com.LastCoderBoy.telegram_youtube_bot.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CommandExecutor {

    public ProcessResult execute(String...  command) throws IOException, InterruptedException {
        log.debug("Executing command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.debug("Command output: {}", line);
            }
        }

        boolean finished = process.waitFor(5, TimeUnit.MINUTES);

        if (! finished) {
            process.destroy();
            throw new InterruptedException("Command execution timed out");
        }

        int exitCode = process.exitValue();
        String outputStr = output.toString();

        log.debug("Command exit code: {}", exitCode);

        return new ProcessResult(exitCode, outputStr);
    }

    public record ProcessResult(int exitCode, String output) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
