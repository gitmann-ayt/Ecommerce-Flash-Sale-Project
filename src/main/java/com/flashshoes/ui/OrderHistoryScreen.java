package com.flashshoes.ui;

import com.flashshoes.model.Customer;
import com.flashshoes.model.Order;
import com.flashshoes.model.OrderItem;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;

/**
 * A real order-history screen (matching the TableView style AdminDashboard
 * already uses for its own tables) instead of a plain text Alert popup.
 * Selecting an order shows its line items - product, size, quantity, price -
 * below the table.
 */
public class OrderHistoryScreen extends Dialog<Void> {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    public OrderHistoryScreen(Customer customer) {
        setTitle("My Orders");
        setHeaderText(customer.getOrderHistory().isEmpty()
                ? "You haven't placed any orders yet."
                : customer.getOrderHistory().size() + " order(s)");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.setPrefWidth(520);
        content.setPrefHeight(420);

        TableView<Order> orderTable = new TableView<>();
        orderTable.setItems(FXCollections.observableArrayList(customer.getOrderHistory()));

        TableColumn<Order, String> idCol = new TableColumn<>("Order ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("orderId"));
        TableColumn<Order, Double> totalCol = new TableColumn<>("Total");
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        TableColumn<Order, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        TableColumn<Order, String> payCol = new TableColumn<>("Payment");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethodUsed"));
        payCol.setPrefWidth(140);

        orderTable.getColumns().addAll(idCol, totalCol, statusCol, payCol);
        VBox.setVgrow(orderTable, Priority.ALWAYS);

        Label itemsHeader = new Label("Items in selected order");
        ListView<String> itemsList = new ListView<>();
        itemsList.setPrefHeight(140);

        orderTable.getSelectionModel().selectedItemProperty().addListener((obs, oldOrder, newOrder) -> {
            itemsList.getItems().clear();
            if (newOrder == null) return;
            for (OrderItem item : newOrder.getItems()) {
                itemsList.getItems().add(String.format("%s (size %s) x%d - $%.2f each",
                        item.getProduct().getName(), item.getSize(), item.getQuantity(), item.getPriceAtPurchase()));
            }
            itemsList.getItems().add("Placed: " + newOrder.getTimestamp().format(DATE_FMT));
        });

        content.getChildren().addAll(orderTable, itemsHeader, itemsList);
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        if (!orderTable.getItems().isEmpty()) {
            orderTable.getSelectionModel().selectFirst();
        }
    }
}