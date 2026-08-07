package com.flashshoes.model;

public class WalletPayment implements PaymentMethod {

    private final Customer customer;

    public WalletPayment(Customer customer) {
        this.customer = customer;
    }

    @Override
    public boolean processPayment(double amount) {
        if (customer.getWalletBalance() >= amount) {
            customer.deductWallet(amount);
            return true;
        }
        return false;
    }

    @Override
    public String getMethodName() { return "Wallet Balance"; }
}
