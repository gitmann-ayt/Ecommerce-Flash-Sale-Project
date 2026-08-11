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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CustomerDashboard extends BorderPane {

    private static final String ALL_CATEGORIES = "All";

    private final DataStore store = DataStore.getInstance();
    private final Customer customer;

    private final FlowPane productGrid = new FlowPane();
    private final HBox categoryBar = new HBox(8);
    private final ToggleGroup categoryGroup = new ToggleGroup();
    private final TextField searchField = new TextField();
    private final VBox cartBox = new VBox(8);
    private final Label cartTotalLabel = new Label();
    private final VBox flashSaleBanner = new VBox(8);
    private final Label welcomeLabel = new Label();

    private String selectedCategory = ALL_CATEGORIES;
    private int knownActiveSales = -1;

    public CustomerDashboard() {
        this.customer = (Customer) store.getCurrentUser();

        setTop(buildTopBar());
        setCenter(buildCenter());
        setRight(buildCartPanel());

        store.flashSaleScheduler.setListener(this::updateFlashSaleStatus);
        store.orderProcessor.setListener(order -> refreshCart());

        refresh();
        knownActiveSales = store.getActiveFlashSales().size();
    }

    private void updateFlashSaleStatus() {
        int activeCount = store.getActiveFlashSales().size();
        if (activeCount != knownActiveSales) {
            knownActiveSales = activeCount;
            refreshFlashSaleBanner();
            refreshProductGrid();
        } else {
            refreshFlashSaleBanner();
        }
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
        historyBtn.getStyleClass().add("secondary");
        historyBtn.setOnAction(e -> showOrderHistory());

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("secondary");
        logoutBtn.setOnAction(e -> {
            customer.logout();
            store.setCurrentUser(null);
            MainApp.showLogin();
        });

        bar.getChildren().addAll(title, welcomeLabel, spacer, historyBtn, logoutBtn);
        return bar;
    }

    private Node buildCenter() {
        VBox center = new VBox(14);
        center.setPadding(new Insets(16));

        flashSaleBanner.setPadding(new Insets(14));
        flashSaleBanner.setStyle("-fx-background-color: #FCE8E6; -fx-background-radius: 8;");

        Label catalogueHeader = new Label("Shop by category");
        catalogueHeader.setFont(Font.font("System", FontWeight.BOLD, 15));

        categoryBar.setAlignment(Pos.CENTER_LEFT);

        searchField.setPromptText("Search by name or brand...");
        searchField.setMaxWidth(320);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> refreshProductGrid());

        HBox filterRow = new HBox(16, categoryBar, searchField);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(categoryBar, Priority.ALWAYS);

        productGrid.setHgap(14);
        productGrid.setVgap(14);
        productGrid.setPadding(new Insets(4));

        ScrollPane scrollPane = new ScrollPane(productGrid);
        scrollPane.setFitToWidth(true);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        center.getChildren().addAll(flashSaleBanner, catalogueHeader, filterRow, scrollPane);
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
        welcomeLabel.setText("Hi, " + customer.getName() + "   Wallet: $" + String.format("%.2f", customer.getWalletBalance()));
        refreshFlashSaleBanner();
        refreshCategoryBar();
        refreshProductGrid();
        refreshCart();
    }

    /** Builds the "All / Casual / Formal / ..." filter chips from whatever categories actually exist right now. */
    private void refreshCategoryBar() {
        categoryBar.getChildren().clear();

        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL_CATEGORIES);
        for (Product p : store.getAllProducts()) {
            categories.add(p.getCategory());
        }

        for (String category : categories) {
            ToggleButton chip = new ToggleButton(category);
            chip.setToggleGroup(categoryGroup);
            chip.getStyleClass().add("category-chip");
            chip.setSelected(category.equals(selectedCategory));
            chip.setOnAction(e -> {
                selectedCategory = category;
                refreshProductGrid();
            });
            categoryBar.getChildren().add(chip);
        }

        // Don't let the user click the active chip and end up with nothing selected.
        categoryGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                categoryGroup.selectToggle(oldToggle);
            }
        });
    }

    private void refreshFlashSaleBanner() {
        flashSaleBanner.getChildren().clear();
        List<FlashSale> active = store.getActiveFlashSales();
        if (active.isEmpty()) {
            Label none = new Label("No flash sales running right now. Check back soon!");
            flashSaleBanner.getChildren().add(none);
            return;
        }
        Label header = new Label("\u26A1 " + active.size() + " flash sale(s) live now");
        header.setFont(Font.font("System", FontWeight.BOLD, 15));
        header.setTextFill(Color.web("#C0392B"));
        flashSaleBanner.getChildren().add(header);

        for (FlashSale sale : active) {
            flashSaleBanner.getChildren().add(buildFlashSaleRow(sale));
        }
    }

    /** One flash sale as a proper row (name, discount badge, stock, timer) instead of one long dashed sentence. */
    private Node buildFlashSaleRow(FlashSale sale) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(6, 10, 6, 10));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 6;");

        Label name = new Label(sale.getProduct().getName());
        name.setFont(Font.font("System", FontWeight.BOLD, 12));
        name.setMaxWidth(220);
        name.setWrapText(true);
        HBox.setHgrow(name, Priority.ALWAYS);

        Label discountBadge = new Label(String.format("%.0f%% OFF", sale.getDiscountPercent()));
        discountBadge.setStyle("-fx-background-color: #C0392B; -fx-text-fill: white; " +
                "-fx-background-radius: 4; -fx-padding: 2 8 2 8; -fx-font-weight: bold; -fx-font-size: 11;");

        Label stockLabel = new Label(sale.getLimitedStock() + " left");
        stockLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11;");

        long secondsLeft = Duration.between(LocalDateTime.now(), sale.getEndTime()).getSeconds();
        String timeText = secondsLeft > 0 ? formatDuration(secondsLeft) : "ending...";
        Label timerLabel = new Label(timeText);
        timerLabel.setStyle("-fx-text-fill: #1F3A5F; -fx-font-weight: bold; -fx-font-size: 11; " +
                "-fx-background-color: #EAF1FB; -fx-background-radius: 4; -fx-padding: 2 8 2 8;");

        row.getChildren().addAll(name, discountBadge, stockLabel, timerLabel);
        return row;
    }

    private String formatDuration(long totalSeconds) {
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (h > 0) return String.format("%dh %dm left", h, m);
        return String.format("%dm %ds left", m, s);
    }

    private void refreshProductGrid() {
        productGrid.getChildren().clear();
        String query = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        for (Product product : store.getAllProducts()) {
            if (!selectedCategory.equals(ALL_CATEGORIES) && !product.getCategory().equals(selectedCategory)) {
                continue;
            }
            if (!query.isEmpty()
                    && !product.getName().toLowerCase().contains(query)
                    && !product.getBrand().toLowerCase().contains(query)) {
                continue;
            }
            productGrid.getChildren().add(buildProductCard(product));
        }
        if (productGrid.getChildren().isEmpty()) {
            productGrid.getChildren().add(new Label("No products match your search/filter."));
        }
    }

    private Node buildProductCard(Product product) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setPrefWidth(190);
        card.getStyleClass().add("product-card");

        ImageView imageView = new ImageView(loadImage(product.getImagePath()));
        imageView.setFitWidth(170);
        imageView.setFitHeight(120);
        imageView.setPreserveRatio(true);

        FlashSale saleForProduct = findActiveSaleFor(product);

        Label name = new Label(product.getName());
        name.setWrapText(true);
        name.setFont(Font.font("System", FontWeight.BOLD, 12));

        Label brand = new Label(product.getBrand() + " \u00B7 " + product.getCategory());
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

        ComboBox<String> sizeBox = new ComboBox<>();
        sizeBox.getItems().addAll(SizeCatalog.sizesFor(product.getGender()));
        sizeBox.setPromptText("Size");
        sizeBox.setMaxWidth(Double.MAX_VALUE);

        Button addBtn = new Button(saleForProduct != null ? "Grab it!" : "Add to Cart");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> {
            String size = sizeBox.getValue();
            if (size == null) {
                sizeBox.setStyle("-fx-border-color: #C0392B; -fx-border-width: 1.5;");
                return;
            }
            sizeBox.setStyle("");
            customer.addToCart(product, size, 1);
            refreshCart();
        });

        card.getChildren().addAll(imageView, name, brand, priceLabel, stockLabel, sizeBox, addBtn);
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
        for (CartLine line : cart.getLines()) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);

            VBox labelBox = new VBox(2);
            Label label = new Label(line.getProduct().getName() + "  (size " + line.getSize() + ")");
            label.setWrapText(true);
            label.setMaxWidth(150);
            Label unitPrice = new Label(String.format("$%.2f each", line.getProduct().getPrice()));
            unitPrice.setStyle("-fx-text-fill: #888; -fx-font-size: 10;");
            labelBox.getChildren().addAll(label, unitPrice);
            HBox.setHgrow(labelBox, Priority.ALWAYS);

            Button minusBtn = new Button("-");
            minusBtn.getStyleClass().add("secondary");
            minusBtn.setMinWidth(26);
            minusBtn.setOnAction(e -> {
                cart.updateQuantity(line, line.getQuantity() - 1);
                refreshCart();
            });

            Label qtyLabel = new Label(String.valueOf(line.getQuantity()));
            qtyLabel.setMinWidth(20);
            qtyLabel.setAlignment(Pos.CENTER);

            Button plusBtn = new Button("+");
            plusBtn.getStyleClass().add("secondary");
            plusBtn.setMinWidth(26);
            plusBtn.setOnAction(e -> {
                cart.updateQuantity(line, line.getQuantity() + 1);
                refreshCart();
            });

            Button removeBtn = new Button("x");
            removeBtn.getStyleClass().add("secondary");
            removeBtn.setOnAction(e -> {
                cart.removeLine(line);
                refreshCart();
            });

            row.getChildren().addAll(labelBox, minusBtn, qtyLabel, plusBtn, removeBtn);
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
                    "Order placed! It's now processing, check 'My Orders' shortly for confirmation.")
                    .showAndWait();
        });
    }

    private void showOrderHistory() {
        new OrderHistoryScreen(customer).showAndWait();
    }
}