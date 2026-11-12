package com.lab.platform.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The six authoritative user roles defined in the Lab Resource Utilization Platform:
 * 1. RESEARCHER_STUDENT (Researcher / Student)
 * 2. LAB_TECHNICIAN (Lab Technician)
 * 3. LAB_MANAGER (Lab Manager)
 * 4. DEPARTMENT_HEAD (Department Head)
 * 5. INSTITUTION_ADMIN (Institution Administrator)
 * 6. SYSTEM_ADMIN (System Administrator)
 */
public enum Role {
    RESEARCHER_STUDENT("Researcher / Student"),
    LAB_TECHNICIAN("Lab Technician"),
    LAB_MANAGER("Lab Manager"),
    DEPARTMENT_HEAD("Department Head"),
    INSTITUTION_ADMIN("Institution Administrator"),
    SYSTEM_ADMIN("System Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonValue
    public String toJson() {
        return name();
    }

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        if ("RESEARCHER".equals(normalized) || "STUDENT".equals(normalized)) {
            return RESEARCHER_STUDENT;
        }
        for (Role role : Role.values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
