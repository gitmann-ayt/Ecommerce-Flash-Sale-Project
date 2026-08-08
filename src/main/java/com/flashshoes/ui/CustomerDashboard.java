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
    private final VBox cartBox = new VBox(6);
    private final Label cartTotalLabel = new Label();
    private final VBox flashSaleBanner = new VBox(5);
    private final Label welcomeLabel = new Label();
    private final TextField searchField = new TextField();
    private final HBox categoryChips = new HBox(8);
    private String selectedCategory = "All";

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
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("👟 FlashShoes");
        title.getStyleClass().add("top-bar-title");

        welcomeLabel.getStyleClass().add("top-bar-subtitle");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField.setPromptText("Search shoes, brands...");
        searchField.getStyleClass().add("combo-box");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, old, val) -> refreshProductGrid());

        Button historyBtn = new Button("My Orders");
        historyBtn.getStyleClass().add("btn-ghost");
        historyBtn.setOnAction(e -> MainApp.showOrderHistory());

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("btn-ghost");
        logoutBtn.setOnAction(e -> {
            customer.logout();
            store.setCurrentUser(null);
            MainApp.showLogin();
        });

        bar.getChildren().addAll(title, welcomeLabel, searchField, spacer, historyBtn, logoutBtn);
        return bar;
    }

    private Node buildCenter() {
        VBox center = new VBox(12);
        center.setPadding(new Insets(16));
        center.setStyle("-fx-background-color: #F7F8FA;");

        flashSaleBanner.getStyleClass().add("flash-banner");

        Label catalogueLabel = new Label("Catalogue");
        catalogueLabel.getStyleClass().add("section-label");

        categoryChips.setPadding(new Insets(0, 0, 4, 0));

        productGrid.setHgap(14);
        productGrid.setVgap(14);
        productGrid.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(productGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        center.getChildren().addAll(flashSaleBanner, catalogueLabel, categoryChips, scrollPane);
        return center;
    }

    private void refreshCategoryChips() {
        categoryChips.getChildren().clear();
        java.util.LinkedHashSet<String> categories = new java.util.LinkedHashSet<>();
        categories.add("All");
        for (Product p : store.getAllProducts()) categories.add(p.getCategory());

        for (String cat : categories) {
            ToggleButton chip = new ToggleButton(cat);
            chip.getStyleClass().add(cat.equals(selectedCategory) ? "btn-primary" : "btn-outline");
            chip.setOnAction(e -> {
                selectedCategory = cat;
                refreshCategoryChips();
                refreshProductGrid();
            });
            categoryChips.getChildren().add(chip);
        }
    }

    private Node buildCartPanel() {
        VBox panel = new VBox(10);
        panel.getStyleClass().add("cart-panel");
        panel.setPrefWidth(260);

        Label header = new Label("Your Cart");
        header.getStyleClass().add("cart-header");

        Separator sep = new Separator();
        sep.getStyleClass().add("cart-divider");

        ScrollPane sp = new ScrollPane(cartBox);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        VBox.setVgrow(sp, Priority.ALWAYS);
        cartBox.setPadding(new Insets(2));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Separator sep2 = new Separator();
        cartTotalLabel.getStyleClass().add("cart-total");

        Button checkoutBtn = new Button("Checkout →");
        checkoutBtn.getStyleClass().add("btn-primary");
        checkoutBtn.setMaxWidth(Double.MAX_VALUE);
        checkoutBtn.setOnAction(e -> doCheckout());

        panel.getChildren().addAll(header, sep, sp, sep2, cartTotalLabel, checkoutBtn);
        return panel;
    }

    private void refresh() {
        welcomeLabel.setText("Hi, " + customer.getName() + "   |   Wallet: $" + String.format("%.2f", customer.getWalletBalance()));
        refreshFlashSaleBanner();
        refreshCategoryChips();
        refreshProductGrid();
        refreshCart();
    }

    private void refreshFlashSaleBanner() {
        flashSaleBanner.getChildren().clear();
        List<FlashSale> active = store.getActiveFlashSales();
        if (active.isEmpty()) {
            Label none = new Label("No flash sales running right now — check back soon!");
            none.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
            flashSaleBanner.getChildren().add(none);
            return;
        }
        Label header = new Label("⚡  " + active.size() + " Flash Sale" + (active.size() > 1 ? "s" : "") + " live now!");
        header.getStyleClass().add("flash-banner-title");
        flashSaleBanner.getChildren().add(header);
        for (FlashSale sale : active) {
            long secondsLeft = Duration.between(LocalDateTime.now(), sale.getEndTime()).getSeconds();
            String timeText = secondsLeft > 0 ? formatDuration(secondsLeft) : "ending...";
            Label line = new Label(String.format("  %s — %.0f%% off, %d left — ends in %s",
                    sale.getProduct().getName(), sale.getDiscountPercent(), sale.getLimitedStock(), timeText));
            line.getStyleClass().add("flash-banner-line");
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
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        for (Product product : store.getAllProducts()) {
            if (!"All".equals(selectedCategory) && !product.getCategory().equals(selectedCategory)) continue;
            if (!query.isEmpty()) {
                String haystack = (product.getName() + " " + product.getBrand()).toLowerCase();
                if (!haystack.contains(query)) continue;
            }
            productGrid.getChildren().add(buildProductCard(product));
        }
        if (productGrid.getChildren().isEmpty()) {
            Label none = new Label("No products match your search.");
            none.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
            productGrid.getChildren().add(none);
        }
    }

    private Node buildProductCard(Product product) {
        VBox card = new VBox(0);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(188);
        card.setOnMouseClicked(e -> MainApp.showProductDetails(product));
        card.setStyle("-fx-cursor: hand;");

        // Image area with rounded top
        StackPane imagePane = new StackPane();
        imagePane.setStyle("-fx-background-color: #F3F4F6; -fx-background-radius: 10 10 0 0;");
        imagePane.setPrefHeight(126);

        ImageView imageView = new ImageView(loadImage(product.getImagePath()));
        imageView.setFitWidth(172);
        imageView.setFitHeight(118);
        imageView.setPreserveRatio(true);
        imagePane.getChildren().add(imageView);

        FlashSale saleForProduct = findActiveSaleFor(product);

        // Sale badge
        if (saleForProduct != null) {
            Label badge = new Label(String.format("%.0f%% OFF", saleForProduct.getDiscountPercent()));
            badge.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-size: 10px; " +
                    "-fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 2 6 2 6;");
            StackPane.setAlignment(badge, Pos.TOP_RIGHT);
            StackPane.setMargin(badge, new Insets(8, 8, 0, 0));
            imagePane.getChildren().add(badge);
        }

        // Info area
        VBox info = new VBox(4);
        info.setPadding(new Insets(10, 10, 10, 10));

        Label name = new Label(product.getName());
        name.getStyleClass().add("product-name");

        Label brand = new Label(product.getBrand() + " · " + product.getCategory());
        brand.getStyleClass().add("product-brand");

        Label priceLabel;
        if (saleForProduct != null) {
            priceLabel = new Label(String.format("$%.2f  (was $%.2f)",
                    saleForProduct.getDiscountedPrice(), product.getPrice()));
            priceLabel.getStyleClass().add("product-price-sale");
        } else {
            priceLabel = new Label(String.format("$%.2f", product.getPrice()));
            priceLabel.getStyleClass().add("product-price");
        }

        Label stockLabel = new Label(saleForProduct != null
                ? saleForProduct.getLimitedStock() + " left at this price!"
                : product.getStockQuantity() + " in stock");
        stockLabel.getStyleClass().add("product-stock");

        Button addBtn = new Button(saleForProduct != null ? "⚡ Grab it!" : "Add to Cart");
        addBtn.getStyleClass().add(saleForProduct != null ? "btn-danger" : "btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        VBox.setMargin(addBtn, new Insets(4, 0, 0, 0));
        addBtn.setOnAction(e -> {
            customer.addToCart(product, 1);
            refreshCart();
        });

        info.getChildren().addAll(name, brand, priceLabel, stockLabel, addBtn);
        card.getChildren().addAll(imagePane, info);
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
        if (f.exists()) return new Image(f.toURI().toString());
        return new Image(getClass().getResourceAsStream("/placeholder.png") == null
                ? "https://via.placeholder.com/170x120?text=Shoe" : "/placeholder.png");
    }

    private void refreshCart() {
        cartBox.getChildren().clear();
        Cart cart = customer.getCart();

        if (cart.getItems().isEmpty()) {
            Label empty = new Label("Your cart is empty");
            empty.getStyleClass().add("cart-empty");
            cartBox.getChildren().add(empty);
            cartTotalLabel.setText("Total: $0.00");
            return;
        }

        for (var entry : cart.getItems().entrySet()) {
            Product p = entry.getKey();
            int qty = entry.getValue();

            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 0, 4, 0));

            VBox itemInfo = new VBox(1);
            Label label = new Label(p.getName());
            label.getStyleClass().add("cart-item-label");
            label.setMaxWidth(160);
            Label qtyLabel = new Label("qty: " + qty);
            qtyLabel.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #9CA3AF;");
            itemInfo.getChildren().addAll(label, qtyLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Button removeBtn = new Button("✕");
            removeBtn.getStyleClass().add("btn-remove");
            removeBtn.setOnAction(e -> {
                cart.removeItem(p);
                refreshCart();
            });

            row.getChildren().addAll(itemInfo, spacer, removeBtn);
            cartBox.getChildren().add(row);

            Separator rowSep = new Separator();
            rowSep.setStyle("-fx-background-color: #F3F4F6;");
            cartBox.getChildren().add(rowSep);
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

}
