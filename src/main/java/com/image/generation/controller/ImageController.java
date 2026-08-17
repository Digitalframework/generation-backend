package com.image.generation.controller;

import com.image.generation.common.ApiExceptions.ConflictException;
import com.image.generation.common.ApiExceptions.NotFoundException;
import com.image.generation.entity.FurnitureImageContent;
import com.image.generation.entity.Room;
import com.image.generation.impl.ImageService;
import com.image.generation.request.ImageMetadataRequest;
import com.image.generation.request.ImageUploadForm;
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
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.util.List;

/**
 * Reading takes a signed-in caller of either role; everything that changes the
 * catalogue is an admin's to do. That split is written on the individual methods
 * below rather than declared once for the class, because the two kinds sit side by
 * side here and a blanket rule would have to be picked apart again anyway.
 *
 * {@code hasAnyRole('USER','ADMIN')} rather than a bare {@code isAuthenticated()}:
 * a token from AuthAndSecurity always carries at least one of the two, so anything
 * else is a caller this application has no notion of, and it should say no rather
 * than guess.
 *
 * The role comes from the {@code roles} claim of the token AuthAndSecurity issued —
 * see {@code com.image.generation.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Images", description = "Upload, browse and serve the furniture pictures")
class ImageController {

    private final ImageService images;

    ImageController(ImageService images) {
        this.images = images;
    }

    @Operation(summary = "Search images", description = "Filter by category, room and/or style; paginated.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The matching page of images"))
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
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping
    Page<FurnitureImageResponse> search(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) Room room,
                                        @RequestParam(required = false) String style,
                                        @Parameter(hidden = true)
                                        @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.DESC)
                                        Pageable pageable) {
        return images.search(categoryId, room, style, pageable);
    }

    @Operation(summary = "Get an image's metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The image's metadata"),
            @ApiResponse(responseCode = "404", description = "No image with this id exists")
    })
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}")
    FurnitureImageResponse get(@PathVariable Long id) throws NotFoundException {
        return images.get(id);
    }

    /**
     * Batch lookup for a known list of ids, e.g. {@code /api/images/batch?ids=1,3,5,6,7}
     * — one indexed {@code IN} query instead of paging through {@link #search} and
     * discarding everything that wasn't asked for. Ids with no matching row are simply
     * absent from the result.
     */
    @Operation(summary = "Get several images' metadata by id")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "The metadata of every id that exists; unknown ids are simply absent"))
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/batch")
    List<FurnitureImageResponse> getBatch(@RequestParam List<Long> ids) {
        return images.get(ids);
    }

    /**
     * Streams the stored blob back as a PNG.
     *
     * A plain {@code <img src="/api/images/42/content">} will not do any more, since an
     * img tag sends no Authorization header: fetch the bytes with the token and give
     * the tag an object URL over the response blob.
     *
     * The one-year {@code Cache-Control} is {@code private}: the browser still keeps
     * the picture, which is what makes a gallery cheap to scroll, but a shared proxy
     * must not — {@code public} would let it hand a stored copy to a caller who never
     * presented a token, which is exactly what requiring one is meant to prevent.
     */
    @Operation(summary = "Get an image's PNG content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The PNG bytes"),
            @ApiResponse(responseCode = "304", description = "The client's cached copy is still current"),
            @ApiResponse(responseCode = "404", description = "No image with this id exists")
    })
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @GetMapping("/{id}/content")
    ResponseEntity<byte[]> content(@PathVariable Long id, WebRequest request) throws NotFoundException {
        FurnitureImageContent content = images.content(id);
        String eTag = "\"" + content.sha256() + "\"";
        if (request.checkNotModified(eTag)) {
            return ResponseEntity.status(304).eTag(eTag).build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .eTag(eTag)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                .header("Content-Disposition",
                        ContentDisposition.inline().filename(content.fileName()).build().toString())
                .body(content.data());
    }

    /**
     * Uploads a 1024x1024 PNG as multipart/form-data: the {@code file} part plus room,
     * category and style. The furniture name and its measurements are read from the file
     * name, e.g. {@code Hoeffner_1_180x90x40.png}.
     */
    @Operation(summary = "Upload a picture",
            description = """
                    Pick a 1024x1024 PNG, the room, the furniture kind and the style.
                    The furniture name and its measurements come from the file name:
                    <name>_<width>x<height>x<depth>.png, e.g. Hoeffner_1_180x90x40.png
                    means Hoeffner_1, 180 cm wide, 90 cm high, 40 cm deep.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "The stored image"),
            @ApiResponse(responseCode = "404", description = "The given room/category combination does not exist")
    })
    // Checked before the multipart body is bound, so a caller who may not upload is
    // turned away without the file being read off the wire at all.
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<FurnitureImageResponse> upload(@Valid @ModelAttribute ImageUploadForm form,
                                                  UriComponentsBuilder uriBuilder)
            throws NotFoundException, ConflictException {
        FurnitureImageResponse created = images.create(form);
        return ResponseEntity
                .created(uriBuilder.path("/api/images/{id}").build(created.id()))
                .body(created);
    }

    @Operation(summary = "Update an image's metadata")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The updated metadata"),
            @ApiResponse(responseCode = "404", description = "No image with this id exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    FurnitureImageResponse updateMetadata(@PathVariable Long id, @Valid @RequestBody ImageMetadataRequest metadata)
            throws NotFoundException {
        return images.update(id, metadata);
    }

    /**
     * Replaces the stored PNG while keeping the metadata.
     */
    @Operation(summary = "Replace an image's PNG content")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The updated metadata"),
            @ApiResponse(responseCode = "404", description = "No image with this id exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}/content", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    FurnitureImageResponse replaceContent(@PathVariable Long id, @RequestPart("file") MultipartFile file)
            throws NotFoundException, ConflictException {
        return images.replaceContent(id, file);
    }

    @Operation(summary = "Delete an image")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "The image was deleted"),
            @ApiResponse(responseCode = "404", description = "No image with this id exists")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable Long id) throws NotFoundException {
        images.delete(id);
        return ResponseEntity.noContent().build();
    }
}
