package com.flashshoes.model;

/**
 * A catalogue item — one shoe listing.
 * stockQuantity is the GENERAL inventory count; a Product currently on flash sale
 * additionally has a separate, more tightly-guarded FlashSale.limitedStock.
 */
public class Product {

    private final String productId;
    private String name;
    private String brand;
    private String category;   // Running, Casual, Formal, Sports, Sandals
    private String gender;     // Men, Women, Unisex
    private double price;
    private int stockQuantity;
    private String imagePath;

    public Product(String productId, String name, String brand, String category,
                    String gender, double price, int stockQuantity, String imagePath) {
        this.productId = productId;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.gender = gender;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.imagePath = imagePath;
    }

    public synchronized void updateStock(int delta) {
        this.stockQuantity += delta;
        if (this.stockQuantity < 0) this.stockQuantity = 0;
    }

    public String getProductId() { return productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public synchronized int getStockQuantity() { return stockQuantity; }
    public synchronized void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return name + " (" + brand + ") - $" + String.format("%.2f", price);
    }
}
