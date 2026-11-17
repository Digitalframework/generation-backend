package com.image.generation.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.image.generation.entity.Room;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Image metadata without the blob. The bytes are served from
 * {@code GET /api/images/{id}/content}.
 */
public record FurnitureImageResponse(
        Long id,
        Long categoryId,
        String categorySlug,
        Room room,
        String categoryName,
        String name,
        String fileName,
        String contentType,
        long sizeBytes,
        int widthPx,
        int heightPx,
        String sha256,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal depthCm,
        String style,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Where the picture itself can be fetched; {@link #contentType} says in which format.
     */
    @JsonProperty("contentUrl")
    public String contentUrl() {
        return "/api/images/" + id + "/content";
    }
}
