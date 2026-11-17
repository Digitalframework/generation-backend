package com.image.generation.impl;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Turns an uploaded multipart file into validated image bytes: it must really be a
 * PNG or a JPEG (magic bytes, not just the declared content type) and it must be
 * 1024x1024.
 */
@Component
public class ImageUpload {

    static final int REQUIRED_SIZE_PX = 1024;

    /**
     * @param data   the raw image bytes, in whichever of the accepted formats they arrived
     * @param sha256 hex digest, used for de-duplication and as the HTTP ETag
     */
    public record UploadedImage(String fileName, String contentType, byte[] data,
                                int widthPx, int heightPx, String sha256) {
    }

    /**
     * What may be stored, and how each is recognised.
     *
     * The leading bytes decide, not the {@code Content-Type} the client sent nor the
     * file extension — both of those are a caller's claim about the file, while these
     * are the file itself. JPEG's three bytes are the start-of-image marker followed by
     * the marker byte of whichever segment comes first, which differs between JFIF and
     * Exif files and so is deliberately not matched any further.
     */
    private enum Format {

        PNG("image/png", "upload.png", (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A),
        JPEG("image/jpeg", "upload.jpg", (byte) 0xFF, (byte) 0xD8, (byte) 0xFF);

        private final String contentType;
        private final String fallbackFileName;
        private final byte[] signature;

        Format(String contentType, String fallbackFileName, int... signature) {
            this.contentType = contentType;
            this.fallbackFileName = fallbackFileName;
            this.signature = new byte[signature.length];
            for (int i = 0; i < signature.length; i++) {
                this.signature[i] = (byte) signature[i];
            }
        }

        static Optional<Format> of(byte[] data) {
            return Arrays.stream(values()).filter(format -> format.matches(data)).findFirst();
        }

        private boolean matches(byte[] data) {
            return data.length >= signature.length
                    && Arrays.equals(data, 0, signature.length, signature, 0, signature.length);
        }
    }

    public UploadedImage read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }

        Format format = Format.of(data)
                .orElseThrow(() -> new IllegalArgumentException("Only PNG and JPEG files are accepted"));

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            throw new IllegalArgumentException("The uploaded file is not a readable " + format);
        }
        if (image.getWidth() != REQUIRED_SIZE_PX || image.getHeight() != REQUIRED_SIZE_PX) {
            throw new IllegalArgumentException("Images must be %dx%d pixels, got %dx%d"
                    .formatted(REQUIRED_SIZE_PX, REQUIRED_SIZE_PX, image.getWidth(), image.getHeight()));
        }

        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? format.fallbackFileName
                : java.nio.file.Paths.get(file.getOriginalFilename()).getFileName().toString();

        return new UploadedImage(fileName, format.contentType, data, image.getWidth(), image.getHeight(), sha256(data));
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
