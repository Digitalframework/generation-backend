package com.image.generation.entity;

/**
 * What the database knows about a stored picture without its bytes: enough to fetch
 * them from the active {@code ImageStorage} and to build the HTTP response headers.
 */
public record FurnitureImageFile(String fileName, String contentType, String sha256) {
}
