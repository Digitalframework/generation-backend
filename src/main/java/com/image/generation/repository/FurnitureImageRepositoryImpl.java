package com.image.generation.repository;

import com.image.generation.entity.FurnitureCategory;
import com.image.generation.entity.FurnitureImage;
import com.image.generation.response.FurnitureImageResponse;
import com.image.generation.entity.Room;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Criteria-based search so that optional filters do not end up as untyped
 * {@code :param is null} checks in the generated SQL.
 * <p>
 * Must stay in the same package as {@link CustomFurnitureImageRepository} and keep the
 * {@code Impl} suffix on the base repository's name ({@code FurnitureImageRepository} +
 * {@code Impl}, not the fragment interface's) — that is how Spring Data finds the
 * fragment implementation. Moving it to another package leaves {@code search} looking
 * like a derived query and fails the context with "No property 'search' found".
 */
class FurnitureImageRepositoryImpl implements CustomFurnitureImageRepository {

    private final EntityManager entityManager;

    FurnitureImageRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<FurnitureImageResponse> search(Long categoryId, Room room, String style, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<FurnitureImageResponse> query = cb.createQuery(FurnitureImageResponse.class);
        Root<FurnitureImage> image = query.from(FurnitureImage.class);
        Join<FurnitureImage, FurnitureCategory> category = image.join("category");

        query.select(cb.construct(FurnitureImageResponse.class,
                        image.get("id"),
                        category.get("id"),
                        category.get("slug"),
                        category.get("room"),
                        category.get("name"),
                        image.get("name"),
                        image.get("fileName"),
                        image.get("contentType"),
                        image.get("sizeBytes"),
                        image.get("widthPx"),
                        image.get("heightPx"),
                        image.get("sha256"),
                        image.get("widthCm"),
                        image.get("heightCm"),
                        image.get("depthCm"),
                        image.get("style"),
                        image.get("createdAt"),
                        image.get("updatedAt")))
                .where(predicates(cb, image, category, categoryId, room, style))
                .orderBy(toOrders(cb, image, category, pageable.getSort()));

        List<FurnitureImageResponse> content = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return PageableExecutionUtils.getPage(content, pageable, () -> count(cb, categoryId, room, style));
    }

    private long count(CriteriaBuilder cb, Long categoryId, Room room, String style) {
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<FurnitureImage> image = query.from(FurnitureImage.class);
        Join<FurnitureImage, FurnitureCategory> category = image.join("category");

        query.select(cb.count(image))
                .where(predicates(cb, image, category, categoryId, room, style));

        return entityManager.createQuery(query).getSingleResult();
    }

    private Predicate[] predicates(CriteriaBuilder cb,
                                   Root<FurnitureImage> image,
                                   Join<FurnitureImage, FurnitureCategory> category,
                                   Long categoryId,
                                   Room room,
                                   String style) {
        List<Predicate> predicates = new ArrayList<>();
        if (categoryId != null) {
            predicates.add(cb.equal(category.get("id"), categoryId));
        }
        if (room != null) {
            predicates.add(cb.equal(category.get("room"), room));
        }
        if (style != null && !style.isBlank()) {
            predicates.add(cb.equal(cb.lower(image.get("style")), style.trim().toLowerCase()));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private List<jakarta.persistence.criteria.Order> toOrders(CriteriaBuilder cb,
                                                              Root<FurnitureImage> image,
                                                              Join<FurnitureImage, FurnitureCategory> category,
                                                              Sort sort) {
        List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            if (!SORTABLE.contains(property)) {
                continue;
            }
            var path = property.startsWith("category.")
                    ? category.get(property.substring("category.".length()))
                    : image.get(property);
            orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
        }
        if (orders.isEmpty()) {
            orders.add(cb.desc(image.get("id")));
        }
        return orders;
    }
}
