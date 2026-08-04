package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class CustomerDashboard extends BorderPane {

    private final DataStore store = DataStore.getInstance();
    private final Customer customer;

    private final FlowPane productGrid = new FlowPane();
    private final VBox cartBox = new VBox(8);
    private final Label cartTotalLabel = new Label();
    private final VBox flashSaleBanner = new VBox(4);
    private final Label welcomeLabel = new Label();

    public CustomerDashboard() {
        this.customer = (Customer) store.getCurrentUser();

        setTop(buildTopBar());
        setCenter(buildCenter());
        setRight(buildCartPanel());

        store.flashSaleScheduler.setListener(this::refresh);
        store.orderProcessor.setListener(order -> refreshCart());

        refresh();
    }

    private Node buildTopBar() {
        HBox bar = new HBox(16);
        bar.setPadding(new Insets(14, 20, 14, 20));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #1F3A5F;");

        Label title = new Label("FlashShoes");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        welcomeLabel.setTextFill(Color.WHITE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button historyBtn = new Button("My Orders");
        historyBtn.setOnAction(e -> showOrderHistory());

        Button logoutBtn = new Button("Log Out");
        logoutBtn.setOnAction(e -> {
            customer.logout();
            store.setCurrentUser(null);
            MainApp.showLogin();
        });

        bar.getChildren().addAll(title, welcomeLabel, spacer, historyBtn, logoutBtn);
        return bar;
    }

    private Node buildCenter() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16));

        flashSaleBanner.setPadding(new Insets(12));
        flashSaleBanner.setStyle("-fx-background-color: #FCE8E6; -fx-background-radius: 8;");

        productGrid.setHgap(14);
        productGrid.setVgap(14);
        productGrid.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(productGrid);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        center.getChildren().addAll(flashSaleBanner, new Label("Catalogue"), scrollPane);
        return center;
    }

    private Node buildCartPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(280);
        panel.setStyle("-fx-background-color: #F4F6F9;");

        Label header = new Label("Your Cart");
        header.setFont(Font.font("System", FontWeight.BOLD, 16));

        ScrollPane sp = new ScrollPane(cartBox);
        sp.setFitToWidth(true);
        VBox.setVgrow(sp, Priority.ALWAYS);
        cartBox.setPadding(new Insets(4));

        cartTotalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        Button checkoutBtn = new Button("Checkout");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> doCheckout());

        panel.getChildren().addAll(header, sp, cartTotalLabel, checkoutBtn);
        return panel;
    }

    private void refresh() {
        welcomeLabel.setText("Hi, " + customer.getName() + "  |  Wallet: $" + String.format("%.2f", customer.getWalletBalance()));
        refreshFlashSaleBanner();
        refreshProductGrid();
        refreshCart();
    }

    private void refreshFlashSaleBanner() {
        flashSaleBanner.getChildren().clear();
        List<FlashSale> active = store.getActiveFlashSales();
        if (active.isEmpty()) {
            Label none = new Label("No flash sales running right now — check back soon!");
            flashSaleBanner.getChildren().add(none);
            return;
        }
        Label header = new Label("⚡ " + active.size() + " Flash Sale(s) live now!");
        header.setFont(Font.font("System", FontWeight.BOLD, 14));
        header.setTextFill(Color.web("#C0392B"));
        flashSaleBanner.getChildren().add(header);
        for (FlashSale sale : active) {
            long secondsLeft = Duration.between(LocalDateTime.now(), sale.getEndTime()).getSeconds();
            String timeText = secondsLeft > 0 ? formatDuration(secondsLeft) : "ending...";
            Label line = new Label(String.format("%s — %.0f%% off, %d left — ends in %s",
                    sale.getProduct().getName(), sale.getDiscountPercent(), sale.getLimitedStock(), timeText));
            flashSaleBanner.getChildren().add(line);
        }
    }

    private String formatDuration(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%dh %dm", h, m);
        return String.format("%dm %ds", m, s);
    }

    private void refreshProductGrid() {
        productGrid.getChildren().clear();
        for (Product product : store.getAllProducts()) {
            productGrid.getChildren().add(buildProductCard(product));
        }
    }

    private Node buildProductCard(Product product) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setPrefWidth(190);
        card.setStyle("-fx-background-color: white; -fx-border-color: #DDD; -fx-border-radius: 6; -fx-background-radius: 6;");

        ImageView imageView = new ImageView(loadImage(product.getImagePath()));
        imageView.setFitWidth(170);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        FlashSale saleForProduct = findActiveSaleFor(product);

        Label name = new Label(product.getName());
        name.setWrapText(true);
        name.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label brand = new Label(product.getBrand() + " · " + product.getCategory());
        brand.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");

        Label priceLabel;
        if (saleForProduct != null) {
            priceLabel = new Label(String.format("$%.2f  (was $%.2f)",
                    saleForProduct.getDiscountedPrice(), product.getPrice()));
            priceLabel.setTextFill(Color.web("#C0392B"));
        } else {
            priceLabel = new Label(String.format("$%.2f", product.getPrice()));
        }
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label stockLabel = new Label(saleForProduct != null
                ? saleForProduct.getLimitedStock() + " left at this price!"
                : product.getStockQuantity() + " in stock");
        stockLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #666;");

        Button addBtn = new Button(saleForProduct != null ? "Grab it!" : "Add to Cart");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            customer.addToCart(product, 1);
            refreshCart();
        });

        card.getChildren().addAll(imageView, name, brand, priceLabel, stockLabel, addBtn);
        return card;
    }

    private FlashSale findActiveSaleFor(Product product) {
        for (FlashSale sale : store.getActiveFlashSales()) {
            if (sale.getProduct().getProductId().equals(product.getProductId())) return sale;
        }
        return null;
    }

    private Image loadImage(String path) {
        File f = new File(path);
        if (f.exists()) {
            return new Image(f.toURI().toString());
        }
        return new Image(getClass().getResourceAsStream("/placeholder.png") == null
                ? "https://via.placeholder.com/170x120?text=Shoe" : "/placeholder.png");
    }

    private void refreshCart() {
        cartBox.getChildren().clear();
        Cart cart = customer.getCart();
        for (var entry : cart.getItems().entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            Label label = new Label(p.getName() + " x" + qty);
            label.setWrapText(true);
            label.setMaxWidth(160);
            Button removeBtn = new Button("x");
            removeBtn.setOnAction(e -> {
                cart.removeItem(p);
                refreshCart();
            });
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(label, spacer, removeBtn);
            cartBox.getChildren().add(row);
        }
        cartTotalLabel.setText("Total: $" + String.format("%.2f", cart.getTotal()));
    }

    private void doCheckout() {
        if (customer.getCart().isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Your cart is empty.").showAndWait();
            return;
        }
        CheckoutDialog dialog = new CheckoutDialog(customer, store);
        dialog.showAndWait().ifPresent(order -> {
            refresh();
            new Alert(Alert.AlertType.INFORMATION,
                    "Order placed! It's now processing — check 'My Orders' shortly for confirmation.")
                    .showAndWait();
        });
    }

    private void showOrderHistory() {
        StringBuilder sb = new StringBuilder();
        for (Order o : customer.getOrderHistory()) {
            sb.append(o.getOrderId()).append(" — $")
              .append(String.format("%.2f", o.getTotalAmount()))
              .append(" — ").append(o.getStatus()).append("\n");
        }
        if (sb.length() == 0) sb.append("No orders yet.");
        new Alert(Alert.AlertType.INFORMATION, sb.toString()).showAndWait();
    }
}
