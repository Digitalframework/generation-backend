package com.image.generation.repository;

import com.image.generation.entity.FurnitureImage;
import com.image.generation.entity.FurnitureImageContent;
import com.image.generation.response.FurnitureImageResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FurnitureImageRepository extends JpaRepository<FurnitureImage, Long>, CustomFurnitureImageRepository {

    /**
     * Metadata only — deliberately avoids selecting the {@code data} column.
     */
    @Query("""
            select new com.image.generation.response.FurnitureImageResponse(
                i.id, c.id, c.slug, c.room, c.name, i.name,
                i.fileName, i.contentType, i.sizeBytes, i.widthPx, i.heightPx, i.sha256,
                i.widthCm, i.heightCm, i.depthCm, i.style, i.createdAt, i.updatedAt)
            from FurnitureImage i
            join i.category c
            where i.id = :id
            """)
    Optional<FurnitureImageResponse> findResponseById(@Param("id") Long id);

    /**
     * Metadata for exactly the requested ids, order unspecified — a single {@code IN}
     * query instead of one page (or several) of {@link #search} filtered client-side.
     * Ids with no matching row are simply absent, not reported.
     */
    @Query("""
            select new com.image.generation.response.FurnitureImageResponse(
                i.id, c.id, c.slug, c.room, c.name, i.name,
                i.fileName, i.contentType, i.sizeBytes, i.widthPx, i.heightPx, i.sha256,
                i.widthCm, i.heightCm, i.depthCm, i.style, i.createdAt, i.updatedAt)
            from FurnitureImage i
            join i.category c
            where i.id in :ids
            """)
    List<FurnitureImageResponse> findResponsesByIdIn(@Param("ids") Collection<Long> ids);

    /**
     * The blob plus just enough to build the HTTP response.
     */
    @Query("""
            select new com.image.generation.entity.FurnitureImageContent(
                i.fileName, i.contentType, i.sha256, i.data)
            from FurnitureImage i
            where i.id = :id
            """)
    Optional<FurnitureImageContent> findContentById(@Param("id") Long id);

    Optional<FurnitureImage> findBySha256(String sha256);

    long countByCategoryId(Long categoryId);
}
