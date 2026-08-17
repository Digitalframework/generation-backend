package com.image.generation.request;

import com.image.generation.entity.Room;

/**
 * What an upload cannot derive from the file itself. The furniture name and its
 * measurements come from the file name — see {@code FurnitureFileName}.
 * <p>
 * Validates itself so the rules hold no matter which caller builds it.
 */
public record ImageUploadRequest(Room room, String category, String style) {

    public ImageUploadRequest {
        if (room == null) {
            throw new IllegalArgumentException("A room is required");
        }
        category = require(category, "category", 64);
        style = require(style, "style", 64);
    }

    private static String require(String value, String what, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A %s is required".formatted(what));
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException("The %s must not be longer than %d characters".formatted(what, maxLength));
        }
        return trimmed;
    }
}
