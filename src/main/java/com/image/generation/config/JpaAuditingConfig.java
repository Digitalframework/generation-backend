package com.image.generation.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Populates the {@code created_at} / {@code updated_at} columns.
 */
@Configuration
@EnableJpaAuditing
class JpaAuditingConfig {
}
