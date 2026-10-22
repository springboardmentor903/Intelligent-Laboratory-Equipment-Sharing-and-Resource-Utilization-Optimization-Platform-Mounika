package com.lab.platform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Clean system startup initializer.
 * No hardcoded accounts, institutions, equipment, or bookings are seeded.
 * All data is dynamically created by users through the application workflows.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Override
    public void run(String... args) {
        logger.info("Application initialized with a clean dynamic schema. Ready to accept live data.");
    }
}
