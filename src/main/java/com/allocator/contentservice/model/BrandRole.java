package com.allocator.contentservice.model;

/**
 * Brand-scoped editorial roles stored in brand_assignments.role.
 * A user's global role (from the auth service) alone is insufficient —
 * they must also have a BrandAssignment for the content's brand.
 *
 * Priority ordering: higher priority = more capabilities.
 * SUPER_ADMIN and ADMIN bypass brand assignment checks entirely.
 */
public enum BrandRole {
    AUTHOR(10),
    FACT_CHECKER(15),
    RESEARCH_REVIEWER(18),
    EDITOR(20),
    COMPLIANCE_OFFICER(25),
    SENIOR_EDITOR(30),
    PUBLISHER(40);

    private final int priority;

    BrandRole(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean hasMinimum(BrandRole minimum) {
        return this.priority >= minimum.priority;
    }

    public static BrandRole fromString(String value) {
        if (value == null) return null;
        try {
            return BrandRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
