package com.flashshoes.service;

import com.flashshoes.model.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Application-wide in-memory state, loaded from CSV at startup via FileManager
 * and written back after every change. Acts as the single source of truth the
 * JavaFX screens read from and write through — keeps GUI classes free of any
 * direct file or threading logic.
 */
public class DataStore {

    private static DataStore instance;

    public final FileManager fileManager = new FileManager("data");
    public final InventoryManager inventoryManager = new InventoryManager();
    public final OrderProcessor orderProcessor = new OrderProcessor();
    public FlashSaleScheduler flashSaleScheduler;

    private final Map<String, Product> productsById = new LinkedHashMap<>();
    private final Map<String, User> usersById = new LinkedHashMap<>();
    private final List<FlashSale> flashSales = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();

    private User currentUser;

    private DataStore() {
        loadAll();
        flashSaleScheduler = new FlashSaleScheduler(flashSales);
        orderProcessor.setPersistenceListener(this::persistOrders);
        orderProcessor.start();
        flashSaleScheduler.start();
    }

    public static synchronized DataStore getInstance() {
        if (instance == null) instance = new DataStore();
        return instance;
    }

    private void loadAll() {
        for (Product p : fileManager.loadProducts()) productsById.put(p.getProductId(), p);
        for (User u : fileManager.loadUsers()) usersById.put(u.getUserId(), u);
        flashSales.addAll(fileManager.loadFlashSales(productsById));
        flashSales.forEach(FlashSale::refreshStatus);

        Map<String, Customer> customersById = new HashMap<>();
        for (User u : usersById.values()) {
            if (u instanceof Customer c) customersById.put(c.getUserId(), c);
        }
        orders.addAll(fileManager.loadOrders(customersById, productsById));
    }

    // ---------------- persistence passthroughs ----------------

    public void persistProducts() { fileManager.saveProducts(productsById.values()); }
    public void persistUsers() { fileManager.saveUsers(usersById.values()); }
    public void persistFlashSales() { fileManager.saveFlashSales(flashSales); }
    public void persistOrders() { fileManager.saveOrders(orders); }

    // ---------------- accessors ----------------

    public List<Product> getAllProducts() { return new ArrayList<>(productsById.values()); }
    public Product getProduct(String id) { return productsById.get(id); }
    public void addProduct(Product p) { productsById.put(p.getProductId(), p); persistProducts(); }
    public void removeProduct(String id) { productsById.remove(id); persistProducts(); }

    public List<FlashSale> getAllFlashSales() { return flashSales; }
    public void addFlashSale(FlashSale sale) { flashSales.add(sale); persistFlashSales(); }
    public List<FlashSale> getActiveFlashSales() {
        List<FlashSale> active = new ArrayList<>();
        for (FlashSale s : flashSales) if (s.isActive()) active.add(s);
        return active;
    }

    public Collection<User> getAllUsers() { return usersById.values(); }
    public User findUserByEmail(String email) {
        for (User u : usersById.values()) if (u.getEmail().equalsIgnoreCase(email)) return u;
        return null;
    }
    public void registerCustomer(Customer c) { usersById.put(c.getUserId(), c); persistUsers(); }

    public List<Order> getAllOrders() { return orders; }
    public void addOrder(Order o) { orders.add(o); persistOrders(); }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User currentUser) { this.currentUser = currentUser; }

    public String nextProductId() { return "PA" + (productsById.size() + 1 + (int) (Math.random() * 1000)); }
    public String nextUserId() { return "U" + (usersById.size() + 1 + (int) (Math.random() * 1000)); }
    public String nextSaleId() { return "FS" + (flashSales.size() + 1); }
    public String nextOrderId() { return "ORD" + System.currentTimeMillis(); }

    public void shutdown() {
        flashSaleScheduler.stop();
        orderProcessor.stop();
    }
}
