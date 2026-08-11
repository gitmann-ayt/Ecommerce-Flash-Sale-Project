package com.flashshoes.model;

public class WalletPayment implements PaymentMethod {

    private final Customer customer;

    public WalletPayment(Customer customer) {
        this.customer = customer;
    }

    @Override
    public void processPayment(double amount) throws PaymentDeclinedException {
        if (customer.getWalletBalance() < amount) {
            throw new PaymentDeclinedException(String.format(
                    "Insufficient wallet balance: have $%.2f, need $%.2f",
                    customer.getWalletBalance(), amount));
        }
        customer.deductWallet(amount);
    }

    @Override
    public String getMethodName() { return "Wallet Balance"; }
}