package com.LastCoderBoy.telegram_youtube_bot.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
    private String title;
    private String videoId;
    private String url;
    private Long duration; // in seconds
    private String thumbnail;
    private String uploader;
}
