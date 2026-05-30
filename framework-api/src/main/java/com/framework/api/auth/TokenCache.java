package com.framework.api.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe cache for auth tokens with TTL-based expiry.
 *
 * <p>Tokens are keyed by an arbitrary string, allowing different auth schemes
 * or different client IDs to coexist if the framework is ever pointed at
 * multiple APIs in one suite. For Phase 3 there's only ever one entry, keyed
 * by the client ID.
 *
 * <p>Expiry is checked on every {@link #get(String)} call against {@link Instant#now()}.
 * Expired entries are evicted lazily — there's no background sweep, because for
 * a test framework with at most a handful of cached tokens at any time, that
 * overhead isn't justified.
 *
 * <p>A small "safety margin" is applied to expiry: a token's effective lifetime
 * is its server-reported TTL minus {@link #EXPIRY_SAFETY_MARGIN}. This avoids
 * a race where the framework decides a token is valid milliseconds before the
 * server starts rejecting it.
 */
public final class TokenCache {

    /** Tokens are considered expired this much earlier than their actual TTL. */
    private static final Duration EXPIRY_SAFETY_MARGIN = Duration.ofSeconds(30);

    private final ConcurrentMap<String, Entry> cache = new ConcurrentHashMap<>();

    /**
     * Stores {@code token} under {@code key} with the given TTL.
     */
    public void put(String key, String token, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(ttl, "ttl");
        Instant expiresAt = Instant.now().plus(ttl).minus(EXPIRY_SAFETY_MARGIN);
        cache.put(key, new Entry(token, expiresAt));
    }

    /**
     * Returns the cached token under {@code key}, or {@code null} if no entry
     * exists or the entry has expired. Expired entries are evicted.
     */
    public String get(String key) {
        Entry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt)) {
            cache.remove(key, entry);   // atomic compare-and-remove
            return null;
        }
        return entry.token;
    }

    /**
     * Removes any entry under {@code key}. Idempotent.
     */
    public void invalidate(String key) {
        cache.remove(key);
    }

    private record Entry(String token, Instant expiresAt) { }
}
