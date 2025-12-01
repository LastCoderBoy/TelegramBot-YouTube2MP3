package com.LastCoderBoy.telegram_youtube_bot.util;

import org.springframework.stereotype.Component;
import java.util.regex.Pattern;

@Component
public class YouTubeUrlValidator {

    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?" +
                    "(youtube\\.com/watch\\? v=|youtu\\.be/|youtube\\.com/embed/|youtube\\.com/v/)" +
                    "([a-zA-Z0-9_-]{11}).*$"
    );

    public boolean isValidYouTubeUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        return YOUTUBE_URL_PATTERN.matcher(url.trim()).matches();
    }

    public String extractVideoId(String url) {
        if (!isValidYouTubeUrl(url)) {
            return null;
        }

        // Handle different YouTube URL formats
        if (url.contains("youtu.be/")) {
            return url.substring(url.indexOf("youtu.be/") + 9,
                    url.indexOf("youtu.be/") + 20);
        } else if (url.contains("watch?v=")) {
            int start = url.indexOf("watch?v=") + 8;
            int end = url.indexOf("&", start);
            if (end == -1) {
                end = Math.min(start + 11, url.length());
            }
            return url.substring(start, end);
        } else if (url.contains("embed/")) {
            int start = url.indexOf("embed/") + 6;
            return url.substring(start, Math.min(start + 11, url.length()));
        }

        return null;
    }
}
