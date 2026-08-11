package com.flashshoes.model;

/**
 * Thrown by a PaymentMethod when a payment cannot be completed (e.g.
 * insufficient wallet balance). Checked, so every PaymentMethod implementation
 * and every caller must explicitly acknowledge the failure case - the kind of
 * compile-time-enforced error handling a boolean return can't give you.
 */
public class PaymentDeclinedException extends Exception {
    public PaymentDeclinedException(String message) {
        super(message);
    }
}