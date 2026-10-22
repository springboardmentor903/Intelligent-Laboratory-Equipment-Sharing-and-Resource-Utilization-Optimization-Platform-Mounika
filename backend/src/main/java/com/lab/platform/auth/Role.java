package com.lab.platform.auth;

/**
 * The six distinct user roles defined in the Lab Resource Utilization Platform specifications:
 * 1. ROLE_RESEARCHER (Researcher / Student)
 * 2. ROLE_LAB_TECHNICIAN (Lab Technician)
 * 3. ROLE_LAB_MANAGER (Lab Manager)
 * 4. ROLE_DEPARTMENT_HEAD (Department Head)
 * 5. ROLE_INSTITUTION_ADMIN (Institution Administrator)
 * 6. ROLE_SYSTEM_ADMIN (System Administrator)
 */
public enum Role {
    ROLE_RESEARCHER("Researcher / Student"),
    ROLE_LAB_TECHNICIAN("Lab Technician"),
    ROLE_LAB_MANAGER("Lab Manager"),
    ROLE_DEPARTMENT_HEAD("Department Head"),
    ROLE_INSTITUTION_ADMIN("Institution Administrator"),
    ROLE_SYSTEM_ADMIN("System Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
