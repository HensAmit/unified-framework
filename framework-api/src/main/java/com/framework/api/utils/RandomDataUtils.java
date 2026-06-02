package com.framework.api.utils;

import com.github.javafaker.Faker;

import java.util.UUID;

/**
 * Generates unique, realistic test data via Java Faker.
 *
 * <p>Used by {@code TestDataFactory} for entity creation and available directly
 * in step definitions. Every value is intended to be unique-enough per call so
 * that parallel scenarios don't collide on, say, a duplicate email.
 *
 * <p><strong>Thread safety:</strong> {@link Faker} is not documented as
 * thread-safe, and Phase 7 runs scenarios in parallel. We therefore hold one
 * {@link Faker} per thread in a {@link ThreadLocal}. Each thread gets its own
 * generator; no shared mutable state across threads.
 */
public final class RandomDataUtils {

    private static final ThreadLocal<Faker> FAKER = ThreadLocal.withInitial(Faker::new);

    private RandomDataUtils() {
        // utility — no instances
    }

    /**
     * Returns a unique email. A short random suffix is appended to the local
     * part so two calls within the same second still differ.
     */
    public static String email() {
        String base = FAKER.get().name().username().replaceAll("[^a-zA-Z0-9]", "");
        return base + "." + alphanumeric(6).toLowerCase() + "@example.com";
    }

    /** Random full name, e.g. "Jane Doe". */
    public static String name() {
        return FAKER.get().name().fullName();
    }

    /** Random phone number. */
    public static String phone() {
        return FAKER.get().phoneNumber().phoneNumber();
    }

    /** A fresh UUID string. */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /** A random alphanumeric string of the requested length. */
    public static String alphanumeric(int length) {
        return FAKER.get().regexify("[A-Za-z0-9]{" + length + "}");
    }
}
