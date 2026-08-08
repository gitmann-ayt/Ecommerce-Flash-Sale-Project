package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckoutDialog extends Dialog<Order> {

    public CheckoutDialog(Customer customer, DataStore store) {
        setTitle("Checkout");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 24, 10, 24));
        content.setMinWidth(320);

        // Total summary row
        Label totalLbl = new Label("Order Total");
        totalLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6B7280;");
        Label totalAmt = new Label("$" + String.format("%.2f", customer.getCart().getTotal()));
        totalAmt.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");
        VBox totalBox = new VBox(2, totalLbl, totalAmt);

        Separator sep = new Separator();

        // Payment method
        Label payLbl = new Label("Payment Method");
        payLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #374151;");

        ToggleGroup group = new ToggleGroup();

        RadioButton cardOpt = styledRadio("💳  Credit Card", group);
        cardOpt.setSelected(true);
        RadioButton walletOpt = styledRadio(
                "👛  Wallet Balance  ($" + String.format("%.2f", customer.getWalletBalance()) + ")", group);
        RadioButton codOpt = styledRadio("🚚  Cash on Delivery", group);

        VBox paymentOptions = new VBox(8, cardOpt, walletOpt, codOpt);
        paymentOptions.setStyle("-fx-background-color: #F9FAFB; -fx-background-radius: 8; " +
                "-fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 12;");

        // Card detail fields — only shown/required when "Credit Card" is selected.
        TextField cardNumberField = new TextField();
        cardNumberField.setPromptText("Card Number (16 digits)");
        TextField expiryField = new TextField();
        expiryField.setPromptText("MM/YY");
        expiryField.setPrefWidth(90);
        TextField cvvField = new TextField();
        cvvField.setPromptText("CVV");
        cvvField.setPrefWidth(70);
        HBox expiryCvvRow = new HBox(8, expiryField, cvvField);
        Label cardError = new Label();
        cardError.setStyle("-fx-text-fill: #DC2626; -fx-font-size: 11px;");
        cardError.setVisible(false);
        cardError.setManaged(false);

        VBox cardDetailsBox = new VBox(8, cardNumberField, expiryCvvRow, cardError);
        cardDetailsBox.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                "-fx-border-color: #E5E7EB; -fx-border-width: 1; -fx-border-radius: 8; -fx-padding: 12;");
        cardDetailsBox.managedProperty().bind(cardDetailsBox.visibleProperty());
        cardDetailsBox.setVisible(cardOpt.isSelected());

        group.selectedToggleProperty().addListener((obs, oldT, newT) ->
                cardDetailsBox.setVisible(newT == cardOpt));

        content.getChildren().addAll(totalBox, sep, payLbl, paymentOptions, cardDetailsBox);
        getDialogPane().setContent(content);

        // Style the dialog pane itself
        getDialogPane().setStyle("-fx-background-color: white;");

        ButtonType placeOrderType = new ButtonType("Place Order", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(placeOrderType, ButtonType.CANCEL);

        // Style the Place Order button via lookup (post-layout)
        Node placeOrderBtn = getDialogPane().lookupButton(placeOrderType);
        placeOrderBtn.getStyleClass().add("btn-primary");

        // Validate card details BEFORE letting the dialog close, so an invalid/missing
        // card can never silently "succeed" and empty the cart.
        placeOrderBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (group.getSelectedToggle() == cardOpt) {
                String number = cardNumberField.getText().replaceAll("\\s", "");
                String expiry = expiryField.getText().trim();
                String cvv = cvvField.getText().trim();

                String error = null;
                if (!number.matches("\\d{16}")) {
                    error = "Enter a valid 16-digit card number.";
                } else if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
                    error = "Enter expiry as MM/YY.";
                } else if (!cvv.matches("\\d{3,4}")) {
                    error = "Enter a valid 3-4 digit CVV.";
                }

                if (error != null) {
                    cardError.setText(error);
                    cardError.setVisible(true);
                    cardError.setManaged(true);
                    event.consume(); // keep the dialog open — do NOT let checkout proceed
                }
            }
        });

        setResultConverter(buttonType -> {
            if (buttonType != placeOrderType) return null;

            String cardLast4 = cardNumberField.getText().length() >= 4
                    ? cardNumberField.getText().replaceAll("\\s", "").substring(cardNumberField.getText().replaceAll("\\s", "").length() - 4)
                    : "0000";

            Cart cart = customer.getCart();
            List<OrderItem> items = new ArrayList<>();
            List<Runnable> rollbacks = new ArrayList<>();

            for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
                Product product = entry.getKey();
                int qty = entry.getValue();
                FlashSale sale = activeSaleFor(store, product);

                boolean reserved;
                double unitPrice;
                if (sale != null) {
                    reserved = store.inventoryManager.reserveFlashSaleStock(sale, qty);
                    unitPrice = sale.getDiscountedPrice();
                    if (reserved) rollbacks.add(() -> store.inventoryManager.releaseFlashSaleStock(sale, qty));
                } else {
                    reserved = store.inventoryManager.reserveStock(product, qty);
                    unitPrice = product.getPrice();
                    if (reserved) rollbacks.add(() -> store.inventoryManager.releaseStock(product, qty));
                }

                if (!reserved) {
                    for (Runnable r : rollbacks) r.run();
                    new Alert(Alert.AlertType.WARNING,
                            "Sorry — " + product.getName() + " just sold out. Please update your cart.")
                            .showAndWait();
                    return null;
                }
                items.add(new OrderItem(product, qty, unitPrice, cart.getSelectedSize(product)));
            }

            double total = 0;
            for (OrderItem i : items) total += i.getLineTotal();

            PaymentMethod method;
            if (group.getSelectedToggle() == walletOpt) {
                method = new WalletPayment(customer);
            } else if (group.getSelectedToggle() == codOpt) {
                method = new CashOnDelivery();
            } else {
                method = new CreditCardPayment(cardLast4);
            }

            boolean paid = method.processPayment(total);
            if (!paid) {
                for (Runnable r : rollbacks) r.run();
                new Alert(Alert.AlertType.WARNING, "Payment failed (insufficient wallet balance?).").showAndWait();
                return null;
            }

            Order order = new Order(store.nextOrderId(), customer, items);
            order.setPaymentMethodUsed(method.getMethodName());
            customer.addOrderToHistory(order);
            store.addOrder(order);
            store.persistProducts();
            store.persistFlashSales();

            cart.clear();
            store.orderProcessor.submit(order);
            return order;
        });
    }

    private RadioButton styledRadio(String text, ToggleGroup group) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(group);
        rb.setStyle("-fx-font-size: 13px; -fx-text-fill: #374151;");
        return rb;
    }

    private FlashSale activeSaleFor(DataStore store, Product product) {
        for (FlashSale sale : store.getActiveFlashSales()) {
            if (sale.getProduct().getProductId().equals(product.getProductId())) return sale;
        }
        return null;
    }
}
