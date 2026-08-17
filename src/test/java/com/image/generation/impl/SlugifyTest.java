package com.image.generation.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlugifyTest {

    @Test
    void buildsSlugsFromRoomAndName() {
        assertThat(CategoryService.slugify("LIVING_ROOM-Coffee table")).isEqualTo("living-room-coffee-table");
        assertThat(CategoryService.slugify("LIVING_ROOM-Sofa")).isEqualTo("living-room-sofa");
    }

    @Test
    void foldsUmlautsAndPunctuation() {
        assertThat(CategoryService.slugify("BEDROOM-Kleiderschrank (groß)")).isEqualTo("bedroom-kleiderschrank-gross");
    }
}
