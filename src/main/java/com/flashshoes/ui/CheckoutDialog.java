package com.flashshoes.ui;

import com.flashshoes.model.*;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Checkout dialog implementing a Saga-like compensating transaction pattern.
 *
 * Invariants this class relies on:
 *  - The charged total is always derived from the reserved OrderItems
 *    (post flash-sale pricing), never from Cart.getTotal(), since the two
 *    can diverge whenever a flash sale is active.
 *  - Every store/payment call that can throw is caught. A thrown exception
 *    from inventoryManager, PaymentMethod, or DataStore is treated the same
 *    as an explicit failure and triggers the same rollback path — nothing
 *    is allowed to leak a reservation or a charge just because it threw
 *    instead of returning false.
 *  - Once store.orderProcessor.submit(order) succeeds, the order is
 *    considered committed. Nothing after that point (i.e. cart.clear())
 *    is allowed to trigger a payment refund or stock release, since the
 *    background processor may already be acting on the order.
 */
public class CheckoutDialog extends Dialog<Order> {

    private static final Logger LOGGER = Logger.getLogger(CheckoutDialog.class.getName());

    private final Customer customer;
    private final DataStore store;
    private final ToggleGroup paymentGroup = new ToggleGroup();
    private final RadioButton cardOpt;
    private final RadioButton walletOpt;
    private final RadioButton codOpt;

    public CheckoutDialog(Customer customer, DataStore store) {
        this.customer = customer;
        this.store = store;
        setTitle("Checkout");
        setHeaderText("Total: $" + String.format("%.2f", customer.getCart().getTotal()));

        cardOpt = new RadioButton("Credit Card");
        cardOpt.setToggleGroup(paymentGroup);
        cardOpt.setSelected(true);

        walletOpt = new RadioButton(
                "Wallet Balance ($" + String.format("%.2f", customer.getWalletBalance()) + ")"
        );
        walletOpt.setToggleGroup(paymentGroup);

        codOpt = new RadioButton("Cash on Delivery");
        codOpt.setToggleGroup(paymentGroup);

        VBox content = new VBox(8, cardOpt, walletOpt, codOpt);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);

        ButtonType placeOrderType = new ButtonType("Place Order", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(placeOrderType, ButtonType.CANCEL);

        Button placeOrderBtn = (Button) getDialogPane().lookupButton(placeOrderType);
        if (customer.getCart().getItems().isEmpty()) {
            placeOrderBtn.setDisable(true);
            setHeaderText("Your cart is empty!");
        }

        setResultConverter(this::handleResult);
    }

    private Order handleResult(ButtonType buttonType) {
        if (buttonType == null || buttonType.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
            return null;
        }

        Cart cart = customer.getCart();
        if (cart.getItems().isEmpty()) {
            showAlert("Your cart is empty. Add items before checking out.");
            return null;
        }

        PaymentMethod method = determinePaymentMethod();
        if (method == null) {
            showAlert("Please select a valid payment method.");
            return null;
        }

        List<Runnable> rollbacks = new ArrayList<>();
        List<OrderItem> items;
        try {
            items = reserveCartItems(cart, rollbacks);
        } catch (ReservationFailedException e) {
            rollback(rollbacks);
            showAlert("Sorry — " + e.getMessage() + " Please update your cart.");
            return null;
        }

        final double total = items.stream().mapToDouble(OrderItem::getLineTotal).sum();

        if (method instanceof WalletPayment && customer.getWalletBalance() < total) {
            rollback(rollbacks);
            showAlert("Insufficient wallet balance. Please choose another method or add funds.");
            return null;
        }

        boolean paid;
        try {
            paid = method.processPayment(total);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment processing threw exception", e);
            paid = false;
        }

        if (!paid) {
            rollback(rollbacks);
            showAlert("Payment failed. Please try again.");
            return null;
        }
        if (method instanceof WalletPayment) {
            rollbacks.add(() -> customer.setWalletBalance(customer.getWalletBalance() + total));
        }

        Order order = new Order(store.nextOrderId(), customer, items);
        order.setPaymentMethodUsed(method.getMethodName());

        try {
            persistData();

            customer.addOrderToHistory(order);
            store.addOrder(order);

            try {
                store.orderProcessor.submit(order);
            } catch (Exception submitFailure) {
                LOGGER.log(Level.SEVERE, "Queue submission failed; undoing order commit", submitFailure);

                //customer.removeOrderFromHistory(order);
                store.removeOrder(order);

                rollback(rollbacks);
                try {
                    persistData();
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to persist rollback state to disk", ex);
                }

                showAlert("Failed to place order. Payment refunded and stock released.");
                return null;
            }

            try {
                cart.clear();
            } catch (Exception cartFailure) {
                LOGGER.log(Level.WARNING,
                        "Order placed successfully but failed to clear cart UI state", cartFailure);
            }

            return order;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Order commit failed before queue submission", e);

            //customer.removeOrderFromHistory(order);
            store.removeOrder(order);

            rollback(rollbacks);
            try {
                persistData();
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to persist rollback state to disk", ex);
            }

            showAlert("Failed to place order. Payment refunded and stock released.");
            return null;
        }
    }

    private List<OrderItem> reserveCartItems(Cart cart, List<Runnable> rollbacks)
            throws ReservationFailedException {
        List<OrderItem> items = new ArrayList<>();

        for (Map.Entry<Product, Integer> entry : cart.getItems().entrySet()) {
            Product product = entry.getKey();
            int qty = entry.getValue();

            Optional<FlashSale> sale = findActiveSale(product);
            boolean reserved;
            double unitPrice;

            try {
                if (sale.isPresent()) {
                    FlashSale fs = sale.get();
                    reserved = store.inventoryManager.reserveFlashSaleStock(fs, qty);
                    unitPrice = fs.getDiscountedPrice();
                    if (reserved) {
                        rollbacks.add(() -> store.inventoryManager.releaseFlashSaleStock(fs, qty));
                    }
                } else {
                    reserved = store.inventoryManager.reserveStock(product, qty);
                    unitPrice = product.getPrice();
                    if (reserved) {
                        rollbacks.add(() -> store.inventoryManager.releaseStock(product, qty));
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Inventory reservation threw for " + product.getName(), e);
                throw new ReservationFailedException(
                        "Couldn't reserve " + product.getName() + " right now.");
            }

            if (!reserved) {
                throw new ReservationFailedException(product.getName() + " just sold out.");
            }

            items.add(new OrderItem(product, qty, unitPrice));
        }

        return items;
    }

    private Optional<FlashSale> findActiveSale(Product product) {
        return store.getActiveFlashSales().stream()
                .filter(sale -> sale.getProduct().getProductId().equals(product.getProductId()))
                .findFirst();
    }

    private PaymentMethod determinePaymentMethod() {
        Toggle selected = paymentGroup.getSelectedToggle();
        if (selected == cardOpt) {
            return new CreditCardPayment("4242");
        } else if (selected == walletOpt) {
            return new WalletPayment(customer);
        } else if (selected == codOpt) {
            return new CashOnDelivery();
        }
        return null;
    }

    private void rollback(List<Runnable> rollbacks) {
        for (int i = rollbacks.size() - 1; i >= 0; i--) {
            try {
                rollbacks.get(i).run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Rollback action failed", e);
            }
        }
    }

    private void persistData() {
        store.persistProducts();
        store.persistFlashSales();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static class ReservationFailedException extends Exception {
        ReservationFailedException(String message) {
            super(message);
        }
    }
}
