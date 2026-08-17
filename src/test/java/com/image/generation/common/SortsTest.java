package com.image.generation.common;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SortsTest {

    private static final Set<String> SORTABLE = Set.of("id", "createdAt", "widthCm", "category.name");
    private static final Sort FALLBACK = Sort.by(Sort.Direction.DESC, "id");

    @Test
    void keepsAValidSort() {
        Sort cleaned = Sorts.clean(Sort.by(Sort.Direction.ASC, "widthCm"), SORTABLE, FALLBACK);

        assertThat(cleaned).containsExactly(Sort.Order.asc("widthCm"));
    }

    @Test
    void stripsTheJsonPunctuationSwaggerSends() {
        Sort cleaned = Sorts.clean(Sort.by(Sort.Direction.DESC, "[\"createdAt"), SORTABLE, FALLBACK);

        assertThat(cleaned).containsExactly(Sort.Order.desc("createdAt"));
    }

    @Test
    void fallsBackWhenThePlaceholderOrAFilterValueEndsUpInSort() {
        assertThat(Sorts.clean(Sort.by("[\"string\"]"), SORTABLE, FALLBACK)).isEqualTo(FALLBACK);
        assertThat(Sorts.clean(Sort.by("Modern"), SORTABLE, FALLBACK)).isEqualTo(FALLBACK);
        assertThat(Sorts.clean(Sort.unsorted(), SORTABLE, FALLBACK)).isEqualTo(FALLBACK);
    }

    @Test
    void matchesCaseInsensitivelyAndKeepsTheCanonicalSpelling() {
        Sort cleaned = Sorts.clean(Sort.by("CREATEDAT"), SORTABLE, FALLBACK);

        assertThat(cleaned).containsExactly(Sort.Order.asc("createdAt"));
    }

    @Test
    void dropsOnlyTheUnusableEntries() {
        Sort cleaned = Sorts.clean(Sort.by("nonsense").and(Sort.by("category.name")), SORTABLE, FALLBACK);

        assertThat(cleaned).containsExactly(Sort.Order.asc("category.name"));
    }
}
