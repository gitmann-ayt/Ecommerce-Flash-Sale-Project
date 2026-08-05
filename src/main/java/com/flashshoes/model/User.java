package com.flashshoes.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Abstract base type shared by every account in the system.
 * Never instantiated directly — only Customer or Admin are created.
 * Demonstrates ABSTRACTION (login/logout is the only public contract callers see)
 * and ENCAPSULATION (passwordHash is never exposed, only verified).
 */
public abstract class User {

    private final String userId;
    private String name;
    private String email;
    private String passwordHash;

    protected User(String userId, String name, String email, String plainPassword) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = hash(plainPassword);
    }

    /** Used only by FileManager when reconstructing a User from a saved CSV row (hash already computed). */
    protected User(String userId, String name, String email, String passwordHash, boolean isHash) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public boolean login(String attemptedPassword) {
        return this.passwordHash.equals(hash(attemptedPassword));
    }

    public void logout() {
        // No server-side session in this desktop app; UI simply returns to LoginScreen.
    }

    protected static String hash(String plain) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(plain.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }

    /** Each subclass reports its own role so FileManager can tag CSV rows and reload the right subtype. */
    public abstract String getRole();

    @Override
    public String toString() {
        return getRole() + "{" + userId + ", " + name + ", " + email + "}";
    }
}
