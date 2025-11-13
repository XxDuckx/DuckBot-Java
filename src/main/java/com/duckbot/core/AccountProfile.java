package com.duckbot.core;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents an account that can be used by bots.
 */
public final class AccountProfile {

    public String id = UUID.randomUUID().toString();
    public String username;
    public String email;
    public String pin;
    public boolean active = true;

    public AccountProfile() {
    }

    @Override
    public String toString() {
        return "AccountProfile{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountProfile that)) return false;
        return active == that.active && Objects.equals(id, that.id) && Objects.equals(username, that.username) && Objects.equals(email, that.email) && Objects.equals(pin, that.pin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, pin, active);
    }
}