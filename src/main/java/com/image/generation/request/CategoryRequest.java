package com.image.generation.request;

import com.image.generation.entity.Room;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(

        @NotNull
        Room room,

        @NotBlank
        @Size(max = 64)
        String name,

        /**
         * Optional; derived from room + name when omitted.
         */
        @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*", message = "must be lowercase words separated by single dashes")
        @Size(max = 96)
        String slug,

        @Size(max = 512)
        String description
) {
}
