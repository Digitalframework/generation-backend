package com.image.generation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Correcting an existing image by hand ({@code PUT /api/images/{id}}). On upload
 * the name and the measurements are read from the file name instead.
 * <p>
 * This is a full replacement of the metadata, so a measurement left out is a
 * measurement cleared — which is how one that was wrong, or was never known, is
 * taken off an image again. The bounds below only apply to a value that is present;
 * a validation annotation passes a null by definition.
 */
public record ImageMetadataRequest(

        @NotNull
        Long categoryId,

        @NotBlank @Size(max = 128)
        String name,

        @DecimalMin("1.0") @DecimalMax("1000") @Digits(integer = 5, fraction = 2)
        @Schema(description = "Width in cm; omit if unknown", nullable = true)
        BigDecimal widthCm,

        @DecimalMin("1.0") @DecimalMax("1000") @Digits(integer = 5, fraction = 2)
        @Schema(description = "Height in cm; omit if unknown", nullable = true)
        BigDecimal heightCm,

        @DecimalMin("1.0") @DecimalMax("1000") @Digits(integer = 5, fraction = 2)
        @Schema(description = "Depth in cm; omit if unknown", nullable = true)
        BigDecimal depthCm,

        @NotBlank @Size(max = 64)
        String style
) {
}
