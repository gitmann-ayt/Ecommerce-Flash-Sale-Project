package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/**
 * Replaces the old plain "My Orders" text popup. Each order is shown as a card
 * with a live status timeline (Placed -> Confirmed -> Packed -> Shipped ->
 * Out for Delivery -> Delivered), driven by OrderProcessor updating status
 * in the background — the screen refreshes automatically via DataStore listener.
 */
public class OrderHistoryScreen extends BorderPane {

    private static final OrderStatus[] TRACK_STAGES = {
            OrderStatus.PENDING, OrderStatus.CONFIRMED, OrderStatus.PACKED,
            OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED
    };
    private static final String[] STAGE_LABELS = {
            "Placed", "Confirmed", "Packed", "Shipped", "Out for Delivery", "Delivered"
    };

    private final DataStore store = DataStore.getInstance();
    private final Customer customer;
    private final Runnable onBack;
    private final VBox ordersBox = new VBox(16);

    public OrderHistoryScreen(Customer customer, Runnable onBack) {
        this.customer = customer;
        this.onBack = onBack;
        setStyle("-fx-background-color: #F7F8FA;");
        setTop(buildTopBar());
        setCenter(buildCenter());

        store.orderProcessor.setListener(order -> refresh());
        refresh();
    }

    private Node buildTopBar() {
        HBox bar = new HBox(16);
        bar.getStyleClass().add("top-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        Button backBtn = new Button("← Back to Catalogue");
        backBtn.getStyleClass().add("btn-ghost");
        backBtn.setOnAction(e -> onBack.run());

        Label title = new Label("My Orders");
        title.getStyleClass().add("top-bar-title");

        bar.getChildren().addAll(backBtn, title);
        return bar;
    }

    private Node buildCenter() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");

        ordersBox.setPadding(new Insets(24));
        ordersBox.setMaxWidth(700);
        scroll.setContent(ordersBox);
        return scroll;
    }

    private void refresh() {
        ordersBox.getChildren().clear();
        List<Order> orders = customer.getOrderHistory();
        if (orders.isEmpty()) {
            Label empty = new Label("You haven't placed any orders yet.");
            empty.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
            ordersBox.getChildren().add(empty);
            return;
        }
        for (int i = orders.size() - 1; i >= 0; i--) {
            ordersBox.getChildren().add(buildOrderCard(orders.get(i)));
        }
    }

    private Node buildOrderCard(Order order) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #E5E7EB; -fx-border-radius: 10;");

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label idLabel = new Label("Order " + order.getOrderId());
        idLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label totalLabel = new Label("$" + String.format("%.2f", order.getTotalAmount()));
        totalLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        header.getChildren().addAll(idLabel, spacer, totalLabel);

        Label itemsLabel = new Label(itemsSummary(order));
        itemsLabel.setWrapText(true);
        itemsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");

        Label paymentLabel = new Label("Paid via " + order.getPaymentMethodUsed());
        paymentLabel.setStyle("-fx-font-size: 11.5px; -fx-text-fill: #9CA3AF;");

        Node timeline = order.getStatus() == OrderStatus.FAILED
                ? failedLabel()
                : buildTimeline(order.getStatus());

        card.getChildren().addAll(header, itemsLabel, paymentLabel, timeline);
        return card;
    }

    private Label failedLabel() {
        Label l = new Label("✕  Order failed — payment or stock issue");
        l.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 12px; -fx-font-weight: bold;");
        return l;
    }

    private String itemsSummary(Order order) {
        StringBuilder sb = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(item.getQuantity()).append("× ").append(item.getProduct().getName());
            if (item.getSelectedSize() != null && !item.getSelectedSize().isBlank()) {
                sb.append(" (").append(item.getSelectedSize()).append(")");
            }
        }
        return sb.toString();
    }

    private Node buildTimeline(OrderStatus current) {
        int currentIndex = indexOf(current);
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < TRACK_STAGES.length; i++) {
            boolean reached = i <= currentIndex;

            VBox step = new VBox(4);
            step.setAlignment(Pos.CENTER);
            step.setPrefWidth(90);

            Label dot = new Label(reached ? "●" : "○");
            dot.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (reached ? "#059669" : "#D1D5DB") + ";");

            Label stageLabel = new Label(STAGE_LABELS[i]);
            stageLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: " + (reached ? "#374151" : "#9CA3AF") +
                    (i == currentIndex ? "; -fx-font-weight: bold;" : ";"));
            stageLabel.setWrapText(true);
            stageLabel.setAlignment(Pos.CENTER);

            step.getChildren().addAll(dot, stageLabel);
            row.getChildren().add(step);

            if (i < TRACK_STAGES.length - 1) {
                Region line = new Region();
                line.setPrefHeight(2);
                line.setPrefWidth(30);
                line.setStyle("-fx-background-color: " + (i < currentIndex ? "#059669" : "#D1D5DB") + ";");
                HBox.setMargin(line, new Insets(-14, 0, 0, -20));
                row.getChildren().add(line);
            }
        }
        return row;
    }

    private int indexOf(OrderStatus status) {
        for (int i = 0; i < TRACK_STAGES.length; i++) if (TRACK_STAGES[i] == status) return i;
        return 0;
    }
}
