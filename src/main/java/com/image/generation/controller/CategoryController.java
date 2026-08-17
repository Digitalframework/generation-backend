package com.image.generation.controller;

import com.image.generation.common.ApiExceptions.ConflictException;
import com.image.generation.common.ApiExceptions.NotFoundException;
import com.image.generation.entity.Room;
import com.image.generation.impl.CategoryService;
import com.image.generation.impl.ImageService;
import com.image.generation.request.CategoryRequest;
import com.image.generation.response.CategoryResponse;
import com.image.generation.response.FurnitureImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "The furniture kinds per room, e.g. LIVING_ROOM + Armchair")
class CategoryController {

    private final CategoryService categories;
    private final ImageService images;

    CategoryController(CategoryService categories, ImageService images) {
        this.categories = categories;
        this.images = images;
    }

    @Operation(summary = "List categories", description = "Optionally filter by room; paginated.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The matching page of categories"))
    @Parameters({
            @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
            @Parameter(name = "size", in = ParameterIn.QUERY, description = "Rows per page",
                    schema = @Schema(type = "integer", defaultValue = "50", minimum = "1")),
            @Parameter(name = "sort", in = ParameterIn.QUERY,
                    description = "property,(asc|desc) — by room then name unless you say otherwise. "
                            + "Sortable: id, room, name, slug, createdAt, updatedAt",
                    array = @ArraySchema(schema = @Schema(type = "string", defaultValue = "room,asc")))
    })
    @GetMapping
    Page<CategoryResponse> list(@RequestParam(required = false) Room room,
                                @Parameter(hidden = true)
                                @PageableDefault(size = 50, sort = {"room", "name"}) Pageable pageable) {
        return categories.list(room, pageable);
    }

    @Operation(summary = "Get a category by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The category"),
            @ApiResponse(responseCode = "404", description = "No category with this id exists")
    })
    @GetMapping("/{id}")
    CategoryResponse get(@PathVariable Long id) throws NotFoundException {
        return categories.get(id);
    }

    @Operation(summary = "Get a category by slug")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The category"),
            @ApiResponse(responseCode = "404", description = "No category with this slug exists")
    })
    @GetMapping("/slug/{slug}")
    CategoryResponse getBySlug(@PathVariable String slug) throws NotFoundException {
        return categories.getBySlug(slug);
    }

    /**
     * Convenience view: all images of one category, newest first.
     */
    @Operation(summary = "List a category's images")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The matching page of images"),
            @ApiResponse(responseCode = "404", description = "No category with this id exists")
    })
    @Parameters({
            @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index",
                    schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")),
            @Parameter(name = "size", in = ParameterIn.QUERY, description = "Rows per page",
                    schema = @Schema(type = "integer", defaultValue = "50", minimum = "1")),
            @Parameter(name = "sort", in = ParameterIn.QUERY,
                    description = "property,(asc|desc) — newest first unless you say otherwise. Sortable: "
                            + "id, name, fileName, sizeBytes, widthCm, heightCm, depthCm, style, createdAt, "
                            + "updatedAt, category.name, category.room, category.slug",
                    array = @ArraySchema(schema = @Schema(type = "string", defaultValue = "id,desc")))
    })
    @GetMapping("/{id}/images")
    Page<FurnitureImageResponse> images(@PathVariable Long id,
                                        @RequestParam(required = false) String style,
                                        @Parameter(hidden = true)
                                        @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)
                                        Pageable pageable) throws NotFoundException {
        categories.get(id);
        return images.search(id, null, style, pageable);
    }

    @Operation(summary = "Create a category")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "The created category"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request,
                                            UriComponentsBuilder uriBuilder) throws ConflictException {
        CategoryResponse created = categories.create(request);
        return ResponseEntity
                .created(uriBuilder.path("/api/categories/{id}").build(created.id()))
                .body(created);
    }

    @Operation(summary = "Update a category")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The updated category"),
            @ApiResponse(responseCode = "404", description = "No category with this id exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request)
            throws NotFoundException, ConflictException {
        return categories.update(id, request);
    }

    @Operation(summary = "Delete a category")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The category was deleted"),
            @ApiResponse(responseCode = "404", description = "No category with this id exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) throws NotFoundException, ConflictException {
        categories.delete(id);
        return ResponseEntity.noContent().build();
    }
}
