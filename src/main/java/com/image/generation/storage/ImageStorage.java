package com.image.generation.storage;

/**
 * Where the picture bytes are kept. Exactly one implementation is active, chosen by
 * {@code app.storage.type} in application.properties:
 * <ul>
 *     <li>{@code blob} - {@link BlobImageStorage}, the bytea column of furniture_image</li>
 *     <li>{@code gcs}  - {@link GcsImageStorage}, a Google Cloud Storage bucket</li>
 * </ul>
 * The metadata always stays in the database; the bytes are addressed by the image's
 * SHA-256, which is unique per stored picture.
 */
public interface ImageStorage {

    /**
     * Stores the bytes of an image whose row has already been flushed.
     */
    void save(String sha256, String contentType, byte[] data);

    /**
     * @throws IllegalStateException if the bytes are missing although the row exists
     */
    byte[] load(String sha256);

    /**
     * Removes the bytes. Must not fail when they are already gone.
     */
    void delete(String sha256);
}
