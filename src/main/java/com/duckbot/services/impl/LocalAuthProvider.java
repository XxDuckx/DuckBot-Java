package com.duckbot.services.impl;

import com.duckbot.services.AuthException;
import com.duckbot.services.AuthProvider;
import com.duckbot.store.JsonStore;
import com.duckbot.util.DataPaths;
import com.google.gson.annotations.SerializedName;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Local JSON-based authentication provider.
 */
public final class LocalAuthProvider implements AuthProvider {

    private final JsonStore store;
    private final Path file;
    private final Argon2 argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id);

    public LocalAuthProvider(JsonStore store) {
        this.store = store;
        this.file = DataPaths.usersFile();
    }

    public boolean requiresAdminSetup() {
        return load().users.isEmpty();
    }

    @Override
    public boolean login(String username, String password) throws AuthException {
        UsersFile usersFile = load();
        Optional<UserRecord> match = usersFile.users.stream()
                .filter(user -> user.username.equalsIgnoreCase(username))
                .findFirst();
        if (match.isEmpty()) {
            throw new AuthException("User not found");
        }
        UserRecord record = match.get();
        if (!argon2.verify(record.pwdHash, password)) {
            throw new AuthException("Invalid credentials");
        }
        return true;
    }

    @Override
    public boolean register(String username, String password) throws AuthException {
        UsersFile usersFile = load();
        if (usersFile.users.stream().anyMatch(user -> user.username.equalsIgnoreCase(username))) {
            throw new AuthException("User already exists");
        }
        PasswordPolicy policy = usersFile.passwordPolicy;
        if (password.length() < policy.minLen) {
            throw new AuthException("Password must be at least " + policy.minLen + " characters");
        }
        if (policy.requireDigits && password.chars().noneMatch(Character::isDigit)) {
            throw new AuthException("Password must contain a digit");
        }
        if (policy.requireSymbols && password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
            throw new AuthException("Password must contain a symbol");
        }
        UserRecord record = new UserRecord();
        record.username = username;
        record.pwdHash = argon2.hash(2, 65536, 1, password);
        record.role = usersFile.users.isEmpty() ? "OWNER" : "USER";
        record.createdUtc = Instant.now().toString();
        usersFile.users.add(record);
        store.write(file, usersFile);
        return true;
    }

    @Override
    public void logout() {
        // No-op for local storage.
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    private UsersFile load() {
        return store.read(file, UsersFile.class).orElseGet(() -> {
            UsersFile usersFile = new UsersFile();
            store.write(file, usersFile);
            return usersFile;
        });
    }

    private static final class UsersFile {
        List<UserRecord> users = new ArrayList<>();
        @SerializedName("password_policy")
        PasswordPolicy passwordPolicy = new PasswordPolicy();
    }

    private static final class UserRecord {
        String username;
        @SerializedName("pwd_hash")
        String pwdHash;
        String role;
        @SerializedName("created_utc")
        String createdUtc;
    }

    private static final class PasswordPolicy {
        int minLen = 8;
        boolean requireDigits = true;
        boolean requireSymbols = false;
    }
}