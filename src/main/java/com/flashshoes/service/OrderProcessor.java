package com.flashshoes.service;

import com.flashshoes.model.Order;
import com.flashshoes.model.OrderStatus;
import javafx.application.Platform;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Consumer side of a producer-consumer pipeline.
 * Cart.checkout() (the producer) enqueues a PENDING Order and returns
 * immediately so the UI never freezes. This background thread (the consumer)
 * dequeues orders one at a time, simulates a payment-gateway delay, and
 * flips the order's status to CONFIRMED or FAILED — decoupling "customer
 * clicked buy" from "order is actually settled".
 */
public class OrderProcessor implements Runnable {

    public interface OrderSettledListener {
        void onOrderSettled(Order order);
    }

    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;
    private OrderSettledListener listener;
    private Thread workerThread;

    public void setListener(OrderSettledListener listener) {
        this.listener = listener;
    }

    public void submit(Order order) {
        orderQueue.offer(order);
    }

    public void start() {
        workerThread = new Thread(this, "OrderProcessor");
        workerThread.setDaemon(true);
        workerThread.start();
    }

    public void stop() {
        running = false;
        if (workerThread != null) workerThread.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                Order order = orderQueue.take(); // blocks until an order is available
                processOrder(order);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void processOrder(Order order) throws InterruptedException {
        Thread.sleep(800); // simulated payment-gateway / fulfilment delay
        order.updateStatus(OrderStatus.CONFIRMED);
        if (listener != null) {
            Platform.runLater(() -> listener.onOrderSettled(order));
        }
    }
}
