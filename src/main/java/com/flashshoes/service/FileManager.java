package com.flashshoes.service;

import com.flashshoes.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Reads and writes every persisted entity as CSV under the data/ folder.
 * Kept as one class so file I/O never leaks into the GUI or model layers,
 * and so save/load stay symmetric and easy to reason about.
 *
 * Files: data/products.csv, data/users.csv, data/orders.csv, data/flashsales.csv
 */
public class FileManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final Path dataDir;

    public FileManager(String dataDirPath) {
        this.dataDir = Paths.get(dataDirPath);
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create data directory: " + dataDir, e);
        }
    }

    private Path path(String filename) { return dataDir.resolve(filename); }

    // ---------------- PRODUCTS ----------------

    public List<Product> loadProducts() {
        List<Product> products = new ArrayList<>();
        List<String[]> rows = readCsv("products.csv");
        for (String[] r : rows) {
            // id,name,brand,category,gender,price,stock,imagePath,sizes,description
            List<String> sizes = (r.length > 8 && !r[8].isBlank())
                    ? Arrays.asList(r[8].split("\\|"))
                    : List.of("S", "M", "L", "XL");
            String description = r.length > 9 ? r[9] : "";
            products.add(new Product(r[0], r[1], r[2], r[3], r[4],
                    Double.parseDouble(r[5]), Integer.parseInt(r[6]), r[7], sizes, description));
        }
        return products;
    }

    public void saveProducts(Collection<Product> products) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id", "name", "brand", "category", "gender", "price", "stock", "imagePath", "sizes", "description"});
        for (Product p : products) {
            rows.add(new String[]{
                    p.getProductId(), p.getName(), p.getBrand(), p.getCategory(), p.getGender(),
                    String.valueOf(p.getPrice()), String.valueOf(p.getStockQuantity()), p.getImagePath(),
                    String.join("|", p.getSizes()), p.getDescription()
            });
        }
        writeCsv("products.csv", rows);
    }

    // ---------------- REVIEWS ----------------

    public List<Review> loadReviews() {
        List<Review> reviews = new ArrayList<>();
        List<String[]> rows = readCsv("reviews.csv");
        for (String[] r : rows) {
            // reviewId,productId,reviewerName,rating,comment,date
            if (r.length < 6) continue;
            reviews.add(new Review(r[0], r[1], r[2], Integer.parseInt(r[3]), r[4],
                    java.time.LocalDate.parse(r[5])));
        }
        return reviews;
    }

    public void saveReviews(Collection<Review> reviews) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"reviewId", "productId", "reviewerName", "rating", "comment", "date"});
        for (Review r : reviews) {
            rows.add(new String[]{
                    r.getReviewId(), r.getProductId(), r.getReviewerName(),
                    String.valueOf(r.getRating()), r.getComment(), r.getDate().toString()
            });
        }
        writeCsv("reviews.csv", rows);
    }

    // ---------------- USERS ----------------

    public List<User> loadUsers() {
        List<User> users = new ArrayList<>();
        List<String[]> rows = readCsv("users.csv");
        for (String[] r : rows) {
            // id,role,name,email,passwordHash,walletBalance
            String role = r[1];
            if (role.equals("ADMIN")) {
                users.add(new Admin(r[0], r[2], r[3], r[4], true));
            } else {
                double wallet = r.length > 5 ? Double.parseDouble(r[5]) : 0.0;
                users.add(new Customer(r[0], r[2], r[3], r[4], true, wallet));
            }
        }
        return users;
    }

    public void saveUsers(Collection<User> users) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id", "role", "name", "email", "passwordHash", "walletBalance"});
        for (User u : users) {
            double wallet = (u instanceof Customer c) ? c.getWalletBalance() : 0.0;
            rows.add(new String[]{
                    u.getUserId(), u.getRole(), u.getName(), u.getEmail(), u.getPasswordHash(),
                    String.valueOf(wallet)
            });
        }
        writeCsv("users.csv", rows);
    }

    // ---------------- FLASH SALES ----------------

    public List<FlashSale> loadFlashSales(Map<String, Product> productsById) {
        List<FlashSale> sales = new ArrayList<>();
        List<String[]> rows = readCsv("flashsales.csv");
        for (String[] r : rows) {
            // id,productId,discountPercent,startTime,endTime,limitedStock,status
            Product product = productsById.get(r[1]);
            if (product == null) continue;
            FlashSale sale = new FlashSale(r[0], product, Double.parseDouble(r[2]),
                    LocalDateTime.parse(r[3], FMT), LocalDateTime.parse(r[4], FMT),
                    Integer.parseInt(r[5]));
            sale.setStatus(SaleStatus.valueOf(r[6]));
            sales.add(sale);
        }
        return sales;
    }

    public void saveFlashSales(Collection<FlashSale> sales) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"id", "productId", "discountPercent", "startTime", "endTime", "limitedStock", "status"});
        for (FlashSale s : sales) {
            rows.add(new String[]{
                    s.getSaleId(), s.getProduct().getProductId(), String.valueOf(s.getDiscountPercent()),
                    s.getStartTime().format(FMT), s.getEndTime().format(FMT),
                    String.valueOf(s.getLimitedStock()), s.getStatus().name()
            });
        }
        writeCsv("flashsales.csv", rows);
    }

    // ---------------- ORDERS ----------------

    public List<Order> loadOrders(Map<String, Customer> customersById, Map<String, Product> productsById) {
        List<Order> orders = new ArrayList<>();
        List<String[]> rows = readCsv("orders.csv");
        Map<String, List<String[]>> byOrderId = new LinkedHashMap<>();
        for (String[] r : rows) {
            // orderId,customerId,productId,quantity,priceAtPurchase,timestamp,status,paymentMethod,size
            byOrderId.computeIfAbsent(r[0], k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<String[]>> entry : byOrderId.entrySet()) {
            List<String[]> lines = entry.getValue();
            String[] first = lines.get(0);
            Customer customer = customersById.get(first[1]);
            if (customer == null) continue;
            List<OrderItem> items = new ArrayList<>();
            for (String[] line : lines) {
                Product product = productsById.get(line[2]);
                if (product == null) continue;
                String size = line.length > 8 ? line[8] : "";
                items.add(new OrderItem(product, Integer.parseInt(line[3]), Double.parseDouble(line[4]), size));
            }
            Order order = new Order(entry.getKey(), customer, items);
            order.updateStatus(OrderStatus.valueOf(first[6]));
            order.setPaymentMethodUsed(first[7]);
            orders.add(order);
        }
        return orders;
    }

    public void saveOrders(Collection<Order> orders) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"orderId", "customerId", "productId", "quantity", "priceAtPurchase", "timestamp", "status", "paymentMethod", "size"});
        for (Order o : orders) {
            for (OrderItem item : o.getItems()) {
                rows.add(new String[]{
                        o.getOrderId(), o.getCustomer().getUserId(), item.getProduct().getProductId(),
                        String.valueOf(item.getQuantity()), String.valueOf(item.getPriceAtPurchase()),
                        o.getTimestamp().format(FMT), o.getStatus().name(),
                        o.getPaymentMethodUsed() == null ? "" : o.getPaymentMethodUsed(),
                        item.getSelectedSize() == null ? "" : item.getSelectedSize()
                });
            }
        }
        writeCsv("orders.csv", rows);
    }

    // ---------------- CSV HELPERS ----------------

    private List<String[]> readCsv(String filename) {
        Path file = path(filename);
        List<String[]> rows = new ArrayList<>();
        if (!Files.exists(file)) return rows;
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                rows.add(parseCsvLine(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filename, e);
        }
        return rows;
    }

    private synchronized void writeCsv(String filename, List<String[]> rows) {
        Path file = path(filename);
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                writer.write(toCsvLine(row));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write " + filename, e);
        }
    }

    private String[] parseCsvLine(String line) {
        // Simple CSV split that respects double-quoted fields containing commas.
        List<String> fields = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        fields.add(cur.toString());
        return fields.toArray(new String[0]);
    }

    private String toCsvLine(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            String f = fields[i] == null ? "" : fields[i];
            if (f.contains(",") || f.contains("\"")) {
                f = "\"" + f.replace("\"", "\"\"") + "\"";
            }
            sb.append(f);
            if (i < fields.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}
