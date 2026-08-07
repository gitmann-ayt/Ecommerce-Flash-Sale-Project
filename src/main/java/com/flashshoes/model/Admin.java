package com.flashshoes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Back-office account. INHERITANCE: extends User.
 * Used to manage the product catalogue and flash sales, and to review all orders.
 */
public class Admin extends User {

    private final List<String> permissions = new ArrayList<>();

    public Admin(String userId, String name, String email, String plainPassword) {
        super(userId, name, email, plainPassword);
        permissions.add("MANAGE_PRODUCTS");
        permissions.add("MANAGE_FLASH_SALES");
        permissions.add("VIEW_ALL_ORDERS");
    }

    public Admin(String userId, String name, String email, String passwordHash, boolean isHash) {
        super(userId, name, email, passwordHash, isHash);
        permissions.add("MANAGE_PRODUCTS");
        permissions.add("MANAGE_FLASH_SALES");
        permissions.add("VIEW_ALL_ORDERS");
    }

    @Override
    public String getRole() { return "ADMIN"; }

    public List<String> getPermissions() { return permissions; }

    public boolean hasPermission(String permission) { return permissions.contains(permission); }
}
