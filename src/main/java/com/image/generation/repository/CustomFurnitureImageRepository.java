package com.image.generation.repository;

import com.image.generation.entity.Room;
import com.image.generation.response.FurnitureImageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public interface CustomFurnitureImageRepository {

    /**
     * What {@code ?sort=} accepts; anything else is dropped by
     * {@code Sorts.clean} before it reaches the query, because an unknown name would
     * end up as an unresolvable Criteria attribute.
     */
    Set<String> SORTABLE = new LinkedHashSet<>(List.of(
            "id", "name", "fileName", "sizeBytes", "widthCm", "heightCm", "depthCm",
            "style", "createdAt", "updatedAt", "category.name", "category.room", "category.slug"));

    /**
     * Newest first.
     */
    Sort DEFAULT_ORDER = Sort.by(Sort.Direction.DESC, "id");

    /**
     * Paged metadata search; every filter is optional (pass {@code null} to ignore it).
     * Never selects the image bytes.
     */
    Page<FurnitureImageResponse> search(Long categoryId, Room room, String style, Pageable pageable);
}
