package com.image.generation.repository;

import com.image.generation.entity.Room;
import com.image.generation.entity.FurnitureCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FurnitureCategoryRepository extends JpaRepository<FurnitureCategory, Long> {

    Page<FurnitureCategory> findAllByRoom(Room room, Pageable pageable);

    Optional<FurnitureCategory> findBySlug(String slug);

    Optional<FurnitureCategory> findByRoomAndNameIgnoreCase(Room room, String name);

    List<FurnitureCategory> findAllByRoomOrderByName(Room room);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByRoomAndNameIgnoreCase(Room room, String name);

    boolean existsByRoomAndNameIgnoreCaseAndIdNot(Room room, String name, Long id);
}
