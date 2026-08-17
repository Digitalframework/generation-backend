package com.image.generation.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Keeps {@code ?sort=} usable from a form that pre-fills it. Swagger UI sends its
 * placeholder as {@code ["string"]} and a hand-typed JSON array arrives as
 * {@code ["createdAt} — both are cleaned up here, and anything that still is not a
 * sortable property is dropped in favour of the endpoint's default order.
 */
public final class Sorts {

    private Sorts() {
    }

    public static Pageable clean(Pageable pageable, Set<String> sortable, Sort fallback) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                clean(pageable.getSort(), sortable, fallback));
    }

    static Sort clean(Sort sort, Set<String> sortable, Sort fallback) {
        List<Sort.Order> kept = new ArrayList<>();
        for (Sort.Order order : sort) {
            canonical(order.getProperty(), sortable).ifPresent(property -> kept.add(order.withProperty(property)));
        }
        return kept.isEmpty() ? fallback : Sort.by(kept);
    }

    /**
     * Strips JSON punctuation and matches case-insensitively, so {@code ["createdAt},
     * {@code "CREATEDAT"} and {@code createdAt} all mean the same property.
     */
    private static java.util.Optional<String> canonical(String property, Set<String> sortable) {
        String stripped = property.replaceAll("[\\[\\]\"']", "").trim();
        return sortable.stream()
                .filter(candidate -> candidate.equalsIgnoreCase(stripped))
                .findFirst();
    }
}
