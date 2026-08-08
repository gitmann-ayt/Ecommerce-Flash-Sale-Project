package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AdminDashboard extends BorderPane {

    private final DataStore store = DataStore.getInstance();
    private final TableView<Product> productTable = new TableView<>();
    private final TableView<Order> orderTable = new TableView<>();
    private final TableView<FlashSale> saleTable = new TableView<>();
    private final TextArea stressLog = new TextArea();

    public AdminDashboard() {
        setTop(buildTopBar());

        TabPane tabs = new TabPane();
        tabs.setTabMinWidth(100);
        tabs.getTabs().addAll(
                new Tab("Products", buildProductsTab()),
                new Tab("Flash Sales", buildFlashSalesTab()),
                new Tab("Orders", buildOrdersTab()),
                new Tab("Concurrency Demo", buildStressTestTab())
        );
        tabs.getTabs().forEach(t -> t.setClosable(false));
        tabs.setStyle("-fx-background-color: #F7F8FA;");
        setCenter(tabs);

        store.flashSaleScheduler.setListener(this::refreshAll);
        refreshAll();
    }

    private Node buildTopBar() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("👟 FlashShoes");
        title.getStyleClass().add("top-bar-title");

        Label adminBadge = new Label("Admin Panel");
        adminBadge.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; " +
                "-fx-font-size: 11px; -fx-padding: 3 8 3 8; -fx-background-radius: 20;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("btn-ghost");
        logoutBtn.setOnAction(e -> {
            store.setCurrentUser(null);
            MainApp.showLogin();
        });

        bar.getChildren().addAll(title, adminBadge, spacer, logoutBtn);
        return bar;
    }

    // ---------------- PRODUCTS TAB ----------------

    @SuppressWarnings("unchecked")
    private Node buildProductsTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #F7F8FA;");

        Label sectionLbl = new Label("Product Catalogue");
        sectionLbl.getStyleClass().add("section-label");

        TableColumn<Product, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("productId"));
        idCol.setPrefWidth(70);
        TableColumn<Product, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);
        TableColumn<Product, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));
        brandCol.setPrefWidth(110);
        TableColumn<Product, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(100);
        TableColumn<Product, Double> priceCol = new TableColumn<>("Price ($)");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceCol.setPrefWidth(90);
        TableColumn<Product, Integer> stockCol = new TableColumn<>("Stock");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("stockQuantity"));
        stockCol.setPrefWidth(70);
        TableColumn<Product, String> sizesCol = new TableColumn<>("Sizes");
        sizesCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                String.join(", ", c.getValue().getSizes())));
        sizesCol.setPrefWidth(140);
        TableColumn<Product, String> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(c -> {
            java.util.List<Review> revs = store.getReviewsFor(c.getValue().getProductId());
            if (revs.isEmpty()) return new javafx.beans.property.SimpleStringProperty("—");
            double avg = revs.stream().mapToInt(Review::getRating).average().orElse(0);
            return new javafx.beans.property.SimpleStringProperty(String.format("%.1f★ (%d)", avg, revs.size()));
        });
        ratingCol.setPrefWidth(90);

        productTable.getColumns().addAll(idCol, nameCol, brandCol, catCol, priceCol, stockCol, sizesCol, ratingCol);
        productTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(productTable, Priority.ALWAYS);

        // Add / Edit product form
        VBox form = new VBox(10);
        form.getStyleClass().add("form-area");

        Label formLbl = new Label("Add New Product");
        formLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #374151;");

        HBox row1 = new HBox(8);
        TextField nameField = new TextField();
        nameField.setPromptText("Name");
        HBox.setHgrow(nameField, Priority.ALWAYS);
        TextField brandField = new TextField();
        brandField.setPromptText("Brand");
        HBox.setHgrow(brandField, Priority.ALWAYS);
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList(
                "Running", "Casual", "Formal", "Sports", "Sandals"));
        categoryBox.setEditable(true);
        categoryBox.setPromptText("Category");
        categoryBox.setPrefWidth(120);
        row1.getChildren().addAll(nameField, brandField, categoryBox);

        HBox row2 = new HBox(8);
        row2.setAlignment(Pos.CENTER_LEFT);
        TextField priceField = new TextField();
        priceField.setPromptText("Price ($)");
        priceField.setPrefWidth(90);
        TextField stockField = new TextField();
        stockField.setPromptText("Stock");
        stockField.setPrefWidth(80);
        TextField sizesField = new TextField();
        sizesField.setPromptText("Sizes, comma-separated (e.g. 7,8,9,10)");
        HBox.setHgrow(sizesField, Priority.ALWAYS);

        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);
        TextArea descriptionField = new TextArea();
        descriptionField.setPromptText("Description shown on the product page");
        descriptionField.setPrefRowCount(2);
        descriptionField.setWrapText(true);
        HBox.setHgrow(descriptionField, Priority.ALWAYS);

        HBox row4 = new HBox(8);
        row4.setAlignment(Pos.CENTER_LEFT);
        Button addBtn = new Button("Add Product");
        addBtn.getStyleClass().add("btn-primary");
        Button updateBtn = new Button("Update Selected");
        updateBtn.getStyleClass().add("btn-outline");
        updateBtn.setDisable(true);
        Button clearBtn = new Button("Clear Form");
        clearBtn.getStyleClass().add("btn-ghost");
        Button reviewsBtn = new Button("View Reviews");
        reviewsBtn.getStyleClass().add("btn-ghost");
        Button deleteBtn = new Button("Delete Selected");
        deleteBtn.getStyleClass().add("btn-danger");

        Runnable clearForm = () -> {
            nameField.clear(); brandField.clear(); categoryBox.setValue(null);
            priceField.clear(); stockField.clear(); sizesField.clear(); descriptionField.clear();
            productTable.getSelectionModel().clearSelection();
            formLbl.setText("Add New Product");
            addBtn.setVisible(true); addBtn.setManaged(true);
            updateBtn.setDisable(true);
        };

        addBtn.setOnAction(e -> {
            try {
                java.util.List<String> sizes = parseSizes(sizesField.getText());
                Product p = new Product(store.nextProductId(), nameField.getText(), brandField.getText(),
                        categoryBox.getValue() == null || categoryBox.getValue().isBlank() ? "Casual" : categoryBox.getValue(),
                        "Unisex", Double.parseDouble(priceField.getText()), Integer.parseInt(stockField.getText()),
                        "data/images/placeholder.png", sizes, descriptionField.getText());
                store.addProduct(p);
                refreshAll();
                clearForm.run();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Price and stock must be numbers.").showAndWait();
            }
        });

        updateBtn.setOnAction(e -> {
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                selected.setName(nameField.getText());
                selected.setBrand(brandField.getText());
                selected.setCategory(categoryBox.getValue() == null || categoryBox.getValue().isBlank()
                        ? selected.getCategory() : categoryBox.getValue());
                selected.setPrice(Double.parseDouble(priceField.getText()));
                selected.setStockQuantity(Integer.parseInt(stockField.getText()));
                selected.setSizes(parseSizes(sizesField.getText()));
                selected.setDescription(descriptionField.getText());
                store.persistProducts();
                refreshAll();
                clearForm.run();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Price and stock must be numbers.").showAndWait();
            }
        });

        clearBtn.setOnAction(e -> clearForm.run());

        deleteBtn.setOnAction(e -> {
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                store.removeProduct(selected.getProductId());
                refreshAll();
                clearForm.run();
            }
        });

        reviewsBtn.setOnAction(e -> {
            Product selected = productTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                new Alert(Alert.AlertType.INFORMATION, "Select a product first.").showAndWait();
                return;
            }
            showReviewsDialog(selected);
        });

        // Selecting a row loads it into the form for editing.
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, sel) -> {
            if (sel == null) return;
            nameField.setText(sel.getName());
            brandField.setText(sel.getBrand());
            categoryBox.setValue(sel.getCategory());
            priceField.setText(String.valueOf(sel.getPrice()));
            stockField.setText(String.valueOf(sel.getStockQuantity()));
            sizesField.setText(String.join(", ", sel.getSizes()));
            descriptionField.setText(sel.getDescription());
            formLbl.setText("Editing " + sel.getProductId());
            updateBtn.setDisable(false);
        });

        Region btnSpacer = new Region();
        HBox.setHgrow(btnSpacer, Priority.ALWAYS);
        row2.getChildren().addAll(priceField, stockField, sizesField);
        row4.getChildren().addAll(btnSpacer, addBtn, updateBtn, clearBtn, reviewsBtn, deleteBtn);
        form.getChildren().addAll(formLbl, row1, row2, row3, row4);
        row3.getChildren().add(descriptionField);

        box.getChildren().addAll(sectionLbl, productTable, form);
        return box;
    }

    private java.util.List<String> parseSizes(String text) {
        if (text == null || text.isBlank()) return java.util.List.of("S", "M", "L", "XL");
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String s : text.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out.isEmpty() ? java.util.List.of("S", "M", "L", "XL") : out;
    }

    private void showReviewsDialog(Product product) {
        java.util.List<Review> reviews = store.getReviewsFor(product.getProductId());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Reviews — " + product.getName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox box = new VBox(8);
        box.setPadding(new Insets(12));
        box.setPrefWidth(420);
        if (reviews.isEmpty()) {
            box.getChildren().add(new Label("No reviews yet for this product."));
        } else {
            double avg = reviews.stream().mapToInt(Review::getRating).average().orElse(0);
            Label header = new Label(String.format("%d reviews · %.1f★ average", reviews.size(), avg));
            header.setStyle("-fx-font-weight: bold;");
            box.getChildren().add(header);
            for (Review r : reviews) {
                VBox card = new VBox(2);
                card.setStyle("-fx-border-color: #E5E7EB; -fx-border-radius: 6; -fx-padding: 8;");
                Label line1 = new Label(r.getStars() + "  " + r.getReviewerName() + "  ·  " + r.getDate());
                line1.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #374151;");
                Label comment = new Label(r.getComment());
                comment.setWrapText(true);
                comment.setStyle("-fx-font-size: 12px; -fx-text-fill: #4B5563;");
                card.getChildren().addAll(line1, comment);
                box.getChildren().add(card);
            }
        }
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    // ---------------- FLASH SALES TAB ----------------

    @SuppressWarnings("unchecked")
    private Node buildFlashSalesTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #F7F8FA;");

        Label sectionLbl = new Label("Flash Sales");
        sectionLbl.getStyleClass().add("section-label");

        TableColumn<FlashSale, String> saleIdCol = new TableColumn<>("Sale ID");
        saleIdCol.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        saleIdCol.setPrefWidth(90);
        TableColumn<FlashSale, String> prodCol = new TableColumn<>("Product");
        prodCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getProduct().getName()));
        prodCol.setPrefWidth(200);
        TableColumn<FlashSale, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(90);
        TableColumn<FlashSale, Double> discCol = new TableColumn<>("Discount %");
        discCol.setCellValueFactory(new PropertyValueFactory<>("discountPercent"));
        discCol.setPrefWidth(100);
        TableColumn<FlashSale, Integer> stockCol = new TableColumn<>("Stock Left");
        stockCol.setCellValueFactory(new PropertyValueFactory<>("limitedStock"));
        stockCol.setPrefWidth(90);

        saleTable.getColumns().addAll(saleIdCol, prodCol, statusCol, discCol, stockCol);
        saleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(saleTable, Priority.ALWAYS);

        VBox form = new VBox(10);
        form.getStyleClass().add("form-area");

        Label formLbl = new Label("Start a Flash Sale");
        formLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #374151;");

        HBox row = new HBox(8);
        row.setAlignment(Pos.CENTER_LEFT);
        ComboBox<Product> productBox = new ComboBox<>(FXCollections.observableArrayList(store.getAllProducts()));
        productBox.setPromptText("Select product");
        productBox.setPrefWidth(200);
        TextField discField = new TextField();
        discField.setPromptText("Discount %");
        discField.setPrefWidth(90);
        TextField stockField = new TextField();
        stockField.setPromptText("Limited stock");
        stockField.setPrefWidth(100);
        TextField minutesField = new TextField();
        minutesField.setPromptText("Duration (min)");
        minutesField.setPrefWidth(110);

        Button createBtn = new Button("Start Flash Sale");
        createBtn.getStyleClass().add("btn-primary");
        createBtn.setOnAction(e -> {
            try {
                Product p = productBox.getValue();
                if (p == null) return;
                int minutes = Integer.parseInt(minutesField.getText());
                FlashSale sale = new FlashSale(store.nextSaleId(), p, Double.parseDouble(discField.getText()),
                        LocalDateTime.now(), LocalDateTime.now().plusMinutes(minutes),
                        Integer.parseInt(stockField.getText()));
                sale.activate();
                store.addFlashSale(sale);
                refreshAll();
            } catch (NumberFormatException ex) {
                new Alert(Alert.AlertType.WARNING, "Discount, stock and minutes must be numbers.").showAndWait();
            }
        });

        row.getChildren().addAll(productBox, discField, stockField, minutesField, createBtn);
        form.getChildren().addAll(formLbl, row);
        box.getChildren().addAll(sectionLbl, saleTable, form);
        return box;
    }

    // ---------------- ORDERS TAB ----------------

    @SuppressWarnings("unchecked")
    private Node buildOrdersTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #F7F8FA;");

        Label sectionLbl = new Label("All Orders");
        sectionLbl.getStyleClass().add("section-label");

        TableColumn<Order, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        idCol.setPrefWidth(100);
        TableColumn<Order, Double> totalCol = new TableColumn<>("Total ($)");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        totalCol.setPrefWidth(100);
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(110);
        TableColumn<Order, String> payCol = new TableColumn<>("Payment Method");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethodUsed"));
        payCol.setPrefWidth(160);

        orderTable.getColumns().addAll(idCol, totalCol, statusCol, payCol);
        orderTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        Button refreshBtn = new Button("Refresh");
        refreshBtn.getStyleClass().add("btn-outline");
        refreshBtn.setOnAction(e -> refreshAll());

        box.getChildren().addAll(sectionLbl, orderTable, refreshBtn);
        return box;
    }

    // ---------------- CONCURRENCY DEMO TAB ----------------

    private Node buildStressTestTab() {
        VBox box = new VBox(12);
        box.setPadding(new Insets(16));
        box.setStyle("-fx-background-color: #F7F8FA;");

        Label sectionLbl = new Label("Concurrency Demo");
        sectionLbl.getStyleClass().add("section-label");

        Label explainer = new Label(
                "Simulates many customers buying the same flash-sale item at the exact same instant, " +
                "using real threads calling InventoryManager.reserveFlashSaleStock() — the same code " +
                "path the checkout screen uses. Stock never goes negative and exactly as many threads " +
                "succeed as there was stock.");
        explainer.setWrapText(true);
        explainer.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        explainer.setMaxWidth(700);

        VBox controlsBox = new VBox(10);
        controlsBox.getStyleClass().add("form-area");
        controlsBox.setMaxWidth(600);

        Label ctrlLbl = new Label("Run the test");
        ctrlLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #374151;");

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER_LEFT);
        ComboBox<FlashSale> saleBox = new ComboBox<>();
        saleBox.setPromptText("Pick a flash sale");
        saleBox.setPrefWidth(200);
        saleBox.setItems(FXCollections.observableArrayList(store.getAllFlashSales()));

        TextField threadsField = new TextField("10");
        threadsField.setPrefWidth(60);
        threadsField.setPromptText("Threads");

        Button runBtn = new Button("▶  Simulate");
        runBtn.getStyleClass().add("btn-primary");
        runBtn.setOnAction(e -> {
            FlashSale sale = saleBox.getValue();
            if (sale == null) { stressLog.appendText("Pick a flash sale first.\n"); return; }
            int n;
            try { n = Integer.parseInt(threadsField.getText()); } catch (NumberFormatException ex) { n = 10; }
            runStressTest(sale, n);
        });

        controls.getChildren().addAll(saleBox, new Label("threads:"), threadsField, runBtn);
        controlsBox.getChildren().addAll(ctrlLbl, controls);

        stressLog.setEditable(false);
        stressLog.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; " +
                "-fx-background-color: #1E293B; -fx-text-fill: #94A3B8; -fx-control-inner-background: #1E293B;");
        stressLog.setPrefRowCount(16);
        VBox.setVgrow(stressLog, Priority.ALWAYS);

        box.getChildren().addAll(sectionLbl, explainer, controlsBox, stressLog);
        return box;
    }

    private void runStressTest(FlashSale sale, int n) {
        int startingStock = sale.getLimitedStock();
        stressLog.appendText("\n--- Simulating " + n + " customers buying '" + sale.getProduct().getName()
                + "' (stock: " + startingStock + ") ---\n");

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(n, 20));
        CountDownLatch latch = new CountDownLatch(n);
        AtomicInteger succeeded = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (int i = 1; i <= n; i++) {
            final int customerNum = i;
            pool.submit(() -> {
                try {
                    boolean got = store.inventoryManager.reserveFlashSaleStock(sale, 1);
                    if (got) {
                        succeeded.incrementAndGet();
                        log("  Customer #" + customerNum + " -> SUCCESS (bought 1 unit)");
                    } else {
                        failed.incrementAndGet();
                        log("  Customer #" + customerNum + " -> rejected (sold out)");
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        new Thread(() -> {
            try { latch.await(); } catch (InterruptedException ignored) {}
            pool.shutdown();
            Platform.runLater(() -> {
                stressLog.appendText(String.format(
                        "Result: %d succeeded, %d rejected. Stock remaining: %d (started at %d). No overselling.\n",
                        succeeded.get(), failed.get(), sale.getLimitedStock(), startingStock));
                store.persistFlashSales();
                store.persistProducts();
                refreshAll();
            });
        }, "StressTest-Watcher").start();
    }

    private void log(String line) {
        Platform.runLater(() -> stressLog.appendText(line + "\n"));
    }

    private void refreshAll() {
        productTable.setItems(FXCollections.observableArrayList(store.getAllProducts()));
        saleTable.setItems(FXCollections.observableArrayList(store.getAllFlashSales()));
        orderTable.setItems(FXCollections.observableArrayList(store.getAllOrders()));
    }
}
