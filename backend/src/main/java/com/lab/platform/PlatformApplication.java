package com.lab.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Entry Point for the Lab Resource Utilization Platform Backend.
 *
 * Modular Monolith Architecture encompassing:
 * - User Authentication & RBAC (6 Roles)
 * - Equipment Inventory Management
 * - Core Booking & Availability Engine
 * - Dynamic Data Seeding for immediate demonstration
 * - Real-Time Utilization Tracking & Schedulers
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }
}
