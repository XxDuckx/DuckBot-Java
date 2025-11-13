package com.duckbot.core;

import java.time.Instant;
import java.util.Objects;

/**
 * Represents an authenticated user.
 */
public final class User {

    public String username;
    public String role = "USER";
    public Instant createdUtc = Instant.now();

    public User() {
    }

    public User(String username, String role, Instant createdUtc) {
        this.username = username;
        this.role = role;
        this.createdUtc = createdUtc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User user)) return false;
        return Objects.equals(username, user.username) && Objects.equals(role, user.role) && Objects.equals(createdUtc, user.createdUtc);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, role, createdUtc);
    }
}