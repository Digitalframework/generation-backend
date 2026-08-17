package com.image.generation.entity;

/**
 * The stored blob plus what is needed for the HTTP response headers.
 */
public record FurnitureImageContent(String fileName, String contentType, String sha256, byte[] data) {
}
