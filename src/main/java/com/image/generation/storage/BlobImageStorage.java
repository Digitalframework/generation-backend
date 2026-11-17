package com.image.generation.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keeps the bytes in the {@code data} column of furniture_image, as before.
 * <p>
 * Goes through JDBC rather than the entity, because the entity is shared with the gcs
 * mode, whose table has no such column. {@link JdbcTemplate} joins the surrounding JPA
 * transaction, so the row and its bytes are committed or rolled back together.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "blob")
class BlobImageStorage implements ImageStorage {

    private final JdbcTemplate jdbc;

    BlobImageStorage(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void save(String sha256, String contentType, byte[] data) {
        int updated = jdbc.update("update furniture_image set data = ? where sha256 = ?", data, sha256);
        if (updated != 1) {
            throw new IllegalStateException("No image row with sha256 " + sha256 + " to store the bytes in");
        }
    }

    @Override
    public byte[] load(String sha256) {
        List<byte[]> rows = jdbc.query("select data from furniture_image where sha256 = ?",
                (rs, rowNum) -> rs.getBytes(1), sha256);
        if (rows.isEmpty() || rows.getFirst() == null) {
            throw new IllegalStateException("No stored bytes for image " + sha256);
        }
        return rows.getFirst();
    }

    /**
     * Nothing to do: the bytes sit in the row, and go with it.
     */
    @Override
    public void delete(String sha256) {
    }
}
