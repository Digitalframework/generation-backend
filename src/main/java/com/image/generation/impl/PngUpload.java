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
import java.util.HexFormat;

/**
 * Turns an uploaded multipart file into validated PNG bytes: it must really be a
 * PNG (magic bytes, not just the declared content type) and it must be 1024x1024.
 */
@Component
public class PngUpload {

    static final int REQUIRED_SIZE_PX = 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    /**
     * @param data   the raw PNG bytes
     * @param sha256 hex digest, used for de-duplication and as the HTTP ETag
     */
    public record Png(String fileName, byte[] data, int widthPx, int heightPx, String sha256) {

        public String contentType() {
            return "image/png";
        }
    }

    public Png read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file was uploaded");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }

        if (!hasPngSignature(data)) {
            throw new IllegalArgumentException("Only PNG files are accepted");
        }

        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            throw new IllegalArgumentException("The uploaded file is not a readable PNG");
        }
        if (image == null) {
            throw new IllegalArgumentException("The uploaded file is not a readable PNG");
        }
        if (image.getWidth() != REQUIRED_SIZE_PX || image.getHeight() != REQUIRED_SIZE_PX) {
            throw new IllegalArgumentException("Images must be %dx%d pixels, got %dx%d"
                    .formatted(REQUIRED_SIZE_PX, REQUIRED_SIZE_PX, image.getWidth(), image.getHeight()));
        }

        String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                ? "upload.png"
                : java.nio.file.Paths.get(file.getOriginalFilename()).getFileName().toString();

        return new Png(fileName, data, image.getWidth(), image.getHeight(), sha256(data));
    }

    private static boolean hasPngSignature(byte[] data) {
        if (data.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
