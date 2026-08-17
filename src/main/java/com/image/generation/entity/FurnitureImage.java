package com.image.generation.entity;

import com.image.generation.impl.FurnitureFileNameAndDimensionParser;
import com.image.generation.repository.FurnitureImageRepository;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single 1024x1024 PNG rendering of a piece of furniture, stored as a bytea blob
 * together with the real-world dimensions and the style it depicts.
 * <p>
 * The {@code data} column is never selected by the metadata queries in
 * {@link FurnitureImageRepository} — only {@code findDataById} reads it.
 */
@Entity
@Table(name = "furniture_image")
@EntityListeners(AuditingEntityListener.class)
public class FurnitureImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private FurnitureCategory category;

    /**
     * The furniture name taken from the upload file name, e.g. {@code Hoeffner_1}.
     */
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width_px", nullable = false)
    private int widthPx;

    @Column(name = "height_px", nullable = false)
    private int heightPx;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "width_cm", nullable = false, precision = 7, scale = 2)
    private BigDecimal widthCm;

    @Column(name = "depth_cm", nullable = false, precision = 7, scale = 2)
    private BigDecimal depthCm;

    @Column(name = "height_cm", nullable = false, precision = 7, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "style", nullable = false, length = 64)
    private String style;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FurnitureImage() {
        // for JPA
    }

    public FurnitureImage(FurnitureCategory category,
                          String name,
                          String fileName,
                          String contentType,
                          int widthPx,
                          int heightPx,
                          byte[] data,
                          String sha256,
                          BigDecimal widthCm,
                          BigDecimal heightCm,
                          BigDecimal depthCm,
                          String style) {
        this.category = category;
        this.name = name;
        this.fileName = fileName;
        this.contentType = contentType;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.data = data;
        this.sizeBytes = data.length;
        this.sha256 = sha256;
        this.widthCm = widthCm;
        this.depthCm = depthCm;
        this.heightCm = heightCm;
        this.style = style;
    }

    /**
     * Replaces the stored PNG. The new file name is authoritative again, so the
     * name and the measurements it encodes are re-applied along with it.
     */
    public void replaceContent(FurnitureFileNameAndDimensionParser parsed,
                               String fileName,
                               String contentType,
                               int widthPx,
                               int heightPx,
                               byte[] data,
                               String sha256) {
        this.name = parsed.name();
        this.widthCm = parsed.widthCm();
        this.heightCm = parsed.heightCm();
        this.depthCm = parsed.depthCm();
        this.fileName = fileName;
        this.contentType = contentType;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.data = data;
        this.sizeBytes = data.length;
        this.sha256 = sha256;
    }

    public Long getId() {
        return id;
    }

    public FurnitureCategory getCategory() {
        return category;
    }

    public void setCategory(FurnitureCategory category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public int getWidthPx() {
        return widthPx;
    }

    public int getHeightPx() {
        return heightPx;
    }

    public String getSha256() {
        return sha256;
    }

    public byte[] getData() {
        return data;
    }

    public BigDecimal getWidthCm() {
        return widthCm;
    }

    public void setWidthCm(BigDecimal widthCm) {
        this.widthCm = widthCm;
    }

    public BigDecimal getDepthCm() {
        return depthCm;
    }

    public void setDepthCm(BigDecimal depthCm) {
        this.depthCm = depthCm;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
