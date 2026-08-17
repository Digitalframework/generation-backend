package com.image.generation.impl;

import com.image.generation.common.ApiExceptions.ConflictException;
import com.image.generation.common.ApiExceptions.NotFoundException;
import com.image.generation.common.Sorts;
import com.image.generation.entity.FurnitureCategory;
import com.image.generation.entity.Room;
import com.image.generation.response.CategoryResponse;
import com.image.generation.repository.FurnitureCategoryRepository;
import com.image.generation.repository.FurnitureImageRepository;
import com.image.generation.request.CategoryRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Sort;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CategoryService implements ICrudBusiness<CategoryRequest, CategoryRequest, CategoryResponse> {

    private final FurnitureCategoryRepository categories;
    private final FurnitureImageRepository images;

    CategoryService(FurnitureCategoryRepository categories, FurnitureImageRepository images) {
        this.categories = categories;
        this.images = images;
    }

    /**
     * What {@code ?sort=} accepts here; see {@link Sorts} for why it is filtered.
     */
    static final Set<String> SORTABLE = new LinkedHashSet<>(
            List.of("id", "room", "name", "slug", "createdAt", "updatedAt"));

    private static final Sort DEFAULT_ORDER = Sort.by("room", "name");

    public Page<CategoryResponse> list(Room room, Pageable pageable) {
        Pageable paging = Sorts.clean(pageable, SORTABLE, DEFAULT_ORDER);
        Page<FurnitureCategory> page = room == null
                ? categories.findAll(paging)
                : categories.findAllByRoom(room, paging);
        return page.map(CategoryResponse::of);
    }

    @Override
    public CategoryResponse get(Long id) throws NotFoundException {
        return CategoryResponse.of(require(id));
    }

    public CategoryResponse getBySlug(String slug) throws NotFoundException {
        return categories.findBySlug(slug)
                .map(CategoryResponse::of)
                .orElseThrow(() -> NotFoundException.of("Category", slug));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public CategoryResponse create(CategoryRequest request) throws ConflictException {
        String slug = slugOf(request);
        if (categories.existsBySlug(slug)) {
            throw new ConflictException("A category with slug '" + slug + "' already exists");
        }
        if (categories.existsByRoomAndNameIgnoreCase(request.room(), request.name().trim())) {
            throw new ConflictException("'" + request.name() + "' already exists in " + request.room());
        }
        FurnitureCategory saved = categories.save(
                new FurnitureCategory(request.room(), request.name().trim(), slug, request.description()));
        return CategoryResponse.of(saved);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public CategoryResponse update(Long id, CategoryRequest request) throws NotFoundException, ConflictException {
        FurnitureCategory category = require(id);
        String slug = slugOf(request);
        if (categories.existsBySlugAndIdNot(slug, id)) {
            throw new ConflictException("A category with slug '" + slug + "' already exists");
        }
        if (categories.existsByRoomAndNameIgnoreCaseAndIdNot(request.room(), request.name().trim(), id)) {
            throw new ConflictException("'" + request.name() + "' already exists in " + request.room());
        }
        category.setRoom(request.room());
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        return CategoryResponse.of(category);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    @Override
    public void delete(Long id) throws NotFoundException, ConflictException {
        FurnitureCategory category = require(id);
        long imageCount = images.countByCategoryId(id);
        if (imageCount > 0) {
            throw new ConflictException(
                    "Category %d still holds %d image(s); delete them first".formatted(id, imageCount));
        }
        categories.delete(category);
    }

    FurnitureCategory require(Long id) throws NotFoundException {
        return categories.findById(id).orElseThrow(() -> NotFoundException.of("Category", id));
    }

    /**
     * Looks a category up the way it is typed in the upload form: room plus the
     * furniture kind, e.g. LIVING_ROOM + "armchair".
     */
    FurnitureCategory require(Room room, String name) throws NotFoundException {
        return categories.findByRoomAndNameIgnoreCase(room, name.trim())
                .orElseThrow(() -> new NotFoundException("There is no '%s' in %s; known are: %s"
                        .formatted(name.trim(), room, knownNames(room))));
    }

    private String knownNames(Room room) {
        List<FurnitureCategory> inRoom = categories.findAllByRoomOrderByName(room);
        return inRoom.isEmpty()
                ? "(none yet — create one via POST /api/categories)"
                : inRoom.stream().map(FurnitureCategory::getName).collect(Collectors.joining(", "));
    }

    private static String slugOf(CategoryRequest request) {
        if (request.slug() != null && !request.slug().isBlank()) {
            return request.slug().trim();
        }
        return slugify(request.room().name() + "-" + request.name());
    }

    /**
     * "LIVING_ROOM-Coffee table" -> "living-room-coffee-table".
     */
    static String slugify(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replace("ß", "ss");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isEmpty()) {
            throw new IllegalArgumentException("Cannot derive a slug from '" + value + "'");
        }
        return slug;
    }
}
