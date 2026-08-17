package com.image.generation.response;

import com.image.generation.entity.FurnitureCategory;
import com.image.generation.entity.Room;

import java.time.Instant;

public record CategoryResponse(
        Long id,
        Room room,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    public static CategoryResponse of(FurnitureCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getRoom(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
