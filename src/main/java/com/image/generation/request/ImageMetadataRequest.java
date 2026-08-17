package com.image.generation.request;

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
 */
public record ImageMetadataRequest(

        @NotNull
        Long categoryId,

        @NotBlank @Size(max = 128)
        String name,

        @NotNull @DecimalMin("1.0") @DecimalMax("1000") @Digits(integer = 5, fraction = 2)
        BigDecimal widthCm,

        @NotNull @DecimalMin("1.0") @DecimalMax("1000") @Digits(integer = 5, fraction = 2)
        BigDecimal heightCm,

        @NotNull @DecimalMin("1.0") @DecimalMax("1000") @Digits(integer = 5, fraction = 2)
        BigDecimal depthCm,

        @NotBlank @Size(max = 64)
        String style
) {
}
