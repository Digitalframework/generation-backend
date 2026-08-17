package com.image.generation.request;

import com.image.generation.entity.Room;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * The multipart upload form: the picture plus the three things that cannot be
 * derived from it. Kept as one object so Swagger UI renders a file picker and
 * three plain fields.
 * <p>
 * The constraints repeat what {@link ImageUploadRequest} enforces in its constructor.
 * They are worth stating twice: here they are reported as a named field of the form,
 * which is what a caller can act on, while the record's own check holds for any caller
 * that builds one without going through a form at all.
 */
public record ImageUploadForm(

        @NotNull
        @Schema(type = "string", format = "binary",
                description = "A 1024x1024 PNG named <name>_<width>x<height>x<depth>.png, e.g. Hoeffner_1_180x90x40.png")
        MultipartFile file,

        @NotNull
        @Schema(description = "The room", example = "LIVING_ROOM")
        Room room,

        @NotBlank @Size(max = 64)
        @Schema(description = "The furniture kind, as listed under /api/categories", example = "Armchair")
        String category,

        @NotBlank @Size(max = 64)
        @Schema(description = "The style the picture shows", example = "Modern")
        String style
) {

    public ImageUploadRequest toRequest() {
        return new ImageUploadRequest(room, category, style);
    }
}
