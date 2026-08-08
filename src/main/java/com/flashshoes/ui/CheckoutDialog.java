package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects a payment method choice, attempts stock reservation through
 * InventoryManager for every line (this is where flash-sale contention is
 * resolved for a real logged-in customer, same code path the stress-test
 * in AdminDashboard exercises with simulated threads), and on success
 * hands the resulting Order to OrderProcessor's queue rather than marking
 * it CONFIRMED immediately.
 */
public class CheckoutDialog extends Dialog<Order> {

    public CheckoutDialog(Customer customer, DataStore store) {
        setTitle("Checkout");
        setHeaderText("Total: $" + String.format("%.2f", customer.getCart().getTotal()));

        ToggleGroup group = new ToggleGroup();
        RadioButton cardOpt = new RadioButton("Credit Card");
        cardOpt.setToggleGroup(group);
        cardOpt.setSelected(true);
        RadioButton walletOpt = new RadioButton("Wallet Balance ($" + String.format("%.2f", customer.getWalletBalance()) + ")");
        walletOpt.setToggleGroup(group);
        RadioButton codOpt = new RadioButton("Cash on Delivery");
        codOpt.setToggleGroup(group);

        VBox content = new VBox(8, cardOpt, walletOpt, codOpt);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);

        ButtonType placeOrderType = new ButtonType("Place Order", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(placeOrderType, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType != placeOrderType) return null;

            Cart cart = customer.getCart();
            List<OrderItem> items = new ArrayList<>();
            List<Runnable> rollbacks = new ArrayList<>();

            // Reserve stock line by line through InventoryManager — this is the
            // thread-safe path; if any line fails (e.g. a flash sale sold out to
            // another thread first), everything reserved so far is rolled back.
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
                            "Sorry, " + product.getName() + " just sold out. Please update your cart.")
                            .showAndWait();
                    return null;
                }
                items.add(new OrderItem(product, qty, unitPrice));
            }

            double total = 0;
            for (OrderItem i : items) total += i.getLineTotal();

            PaymentMethod method;
            if (group.getSelectedToggle() == walletOpt) {
                method = new WalletPayment(customer);
            } else if (group.getSelectedToggle() == codOpt) {
                method = new CashOnDelivery();
            } else {
                method = new CreditCardPayment("4242");
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

            // Hand off to the background OrderProcessor instead of confirming inline —
            // producer side of the producer-consumer pipeline.
            store.orderProcessor.submit(order);

            return order;
        });
    }

    private FlashSale activeSaleFor(DataStore store, Product product) {
        for (FlashSale sale : store.getActiveFlashSales()) {
            if (sale.getProduct().getProductId().equals(product.getProductId())) return sale;
        }
        return null;
    }
}
