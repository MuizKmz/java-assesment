package com.maybank.assessment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kept separate from the main application class so web slice tests (@WebMvcTest) don't require JPA.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
