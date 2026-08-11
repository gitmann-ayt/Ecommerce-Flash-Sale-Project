package com.flashshoes.model;

/**
 * Thrown by User.login() when the supplied password doesn't match. A checked
 * exception rather than a boolean return - this is a genuine custom exception
 * type demonstrating OOP error handling, not just a renamed error code.
 */
public final class InvalidCredentialsException extends Exception {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}