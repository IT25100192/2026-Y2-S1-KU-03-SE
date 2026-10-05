package com.starvoicelanka.contestant.service;

import com.starvoicelanka.common.exception.BadRequestException;
import com.starvoicelanka.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Service
public class MediaStorageService {

    private static final Logger log = LoggerFactory.getLogger(MediaStorageService.class);

    private static final Map<String, String> ALLOWED_MEDIA = Map.of(
            "video/mp4", ".mp4",
            "video/webm", ".webm",
            "video/quicktime", ".mov",
            "audio/mpeg", ".mp3",
            "audio/mp4", ".m4a",
            "audio/wav", ".wav",
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );

    private final AppProperties properties;

    public MediaStorageService(AppProperties properties) {
        this.properties = properties;
        try {
            Files.createDirectories(Paths.get(properties.getMediaDir()));
        } catch (IOException e) {
            log.error("Could not create media directory: {}", e.getMessage());
        }
    }

    public record StoredMedia(String mediaUrl, String mediaMime, long sizeBytes, String originalName) {}

    public StoredMedia storeMedia(Long roundId, Long contestantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file was uploaded");
        }

        String mimeType = file.getContentType();
        if (mimeType == null || !ALLOWED_MEDIA.containsKey(mimeType.toLowerCase())) {
            throw new BadRequestException((mimeType != null ? mimeType : "Unknown") +
                    " is not an accepted format. Use one of: " + String.join(", ", ALLOWED_MEDIA.keySet()));
        }

        if (file.getSize() > properties.getMediaMaxBytes()) {
            double mb = file.getSize() / 1024.0 / 1024.0;
            double limitMb = properties.getMediaMaxBytes() / 1024.0 / 1024.0;
            throw new BadRequestException(String.format("That file is %.1f MB. The limit is %.0f MB.", mb, limitMb));
        }

        String ext = ALLOWED_MEDIA.get(mimeType.toLowerCase());
        String filename = String.format("%s-%s-%s%s", roundId, contestantId, System.currentTimeMillis(), ext);
        Path targetPath = Paths.get(properties.getMediaDir(), filename);

        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store media file: {}", e.getMessage());
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }

        return new StoredMedia("/media/" + filename, mimeType, file.getSize(), file.getOriginalFilename());
    }

    public void deleteMedia(String mediaUrl) {
        if (mediaUrl == null || !mediaUrl.startsWith("/media/")) return;
        String filename = mediaUrl.substring("/media/".length());
        Path path = Paths.get(properties.getMediaDir(), filename);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Could not delete media file: {}", path);
        }
    }
}
