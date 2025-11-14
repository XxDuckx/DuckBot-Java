package com.duckbot.services.impl;

import com.duckbot.core.User;
import com.duckbot.core.UserTier;
import com.duckbot.services.AuthException;
import com.duckbot.services.AuthProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Cloud-based authentication provider with server-side tier validation.
 * Validates user credentials and subscription tiers against backend API.
 */
public final class CloudAuthProvider implements AuthProvider {

    private final String apiBaseUrl;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    private String authToken;
    private User currentUser;

    public CloudAuthProvider(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean login(String username, String password) throws AuthException {
        try {
            JsonObject loginRequest = new JsonObject();
            loginRequest.addProperty("username", username);
            loginRequest.addProperty("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(loginRequest)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
                authToken = responseBody.get("token").getAsString();
                
                // Parse user data from response
                JsonObject userData = responseBody.getAsJsonObject("user");
                String role = userData.get("role").getAsString();
                String tierStr = userData.has("tier") ? userData.get("tier").getAsString() : "FREE";
                UserTier tier = parseTier(tierStr);
                
                currentUser = new User(username, role, tier, Instant.now());
                return true;
            } else if (response.statusCode() == 401) {
                throw new AuthException("Invalid credentials");
            } else if (response.statusCode() == 403) {
                JsonObject errorBody = gson.fromJson(response.body(), JsonObject.class);
                String reason = errorBody.has("message") ? errorBody.get("message").getAsString() : "Account suspended or subscription expired";
                throw new AuthException(reason);
            } else {
                throw new AuthException("Server error: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new AuthException("Connection failed: " + e.getMessage());
        }
    }

    @Override
    public boolean register(String username, String password) throws AuthException {
        try {
            JsonObject registerRequest = new JsonObject();
            registerRequest.addProperty("username", username);
            registerRequest.addProperty("password", password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/auth/register"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(registerRequest)))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                return true;
            } else if (response.statusCode() == 409) {
                throw new AuthException("Username already exists");
            } else {
                JsonObject errorBody = gson.fromJson(response.body(), JsonObject.class);
                String message = errorBody.has("message") ? errorBody.get("message").getAsString() : "Registration failed";
                throw new AuthException(message);
            }
        } catch (IOException | InterruptedException e) {
            throw new AuthException("Connection failed: " + e.getMessage());
        }
    }

    /**
     * Validate current session and refresh tier status.
     */
    public Optional<User> validateSession() throws AuthException {
        if (authToken == null || currentUser == null) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/auth/validate"))
                    .header("Authorization", "Bearer " + authToken)
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonObject responseBody = gson.fromJson(response.body(), JsonObject.class);
                JsonObject userData = responseBody.getAsJsonObject("user");
                
                // Update tier from server (in case of subscription changes)
                String tierStr = userData.has("tier") ? userData.get("tier").getAsString() : "FREE";
                currentUser.tier = parseTier(tierStr);
                
                return Optional.of(currentUser);
            } else {
                authToken = null;
                currentUser = null;
                return Optional.empty();
            }
        } catch (IOException | InterruptedException e) {
            throw new AuthException("Session validation failed: " + e.getMessage());
        }
    }

    @Override
    public void logout() {
        if (authToken != null) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + "/auth/logout"))
                        .header("Authorization", "Bearer " + authToken)
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .timeout(Duration.ofSeconds(5))
                        .build();
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                // Ignore logout errors
            }
        }
        authToken = null;
        currentUser = null;
    }

    @Override
    public boolean isOnline() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/health"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get current authenticated user with updated tier.
     */
    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    /**
     * Parse tier string to UserTier enum.
     */
    private UserTier parseTier(String tierStr) {
        try {
            return UserTier.valueOf(tierStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UserTier.FREE;
        }
    }
}