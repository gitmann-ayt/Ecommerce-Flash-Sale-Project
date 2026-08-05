package com.flashshoes.service;

import com.flashshoes.model.FlashSale;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Background thread that ticks once per second, advances every FlashSale's
 * status (UPCOMING -> ACTIVE -> ENDED) based on wall-clock time, and notifies
 * a UI listener so the JavaFX screen can refresh countdowns.
 *
 * IMPORTANT: this thread never touches JavaFX UI nodes directly. It only
 * calls Platform.runLater(...) so the actual UI update happens back on the
 * JavaFX Application Thread — mirroring Swing's SwingUtilities.invokeLater,
 * but for JavaFX. Skipping this and touching a Label from this thread would
 * be a classic concurrency bug worth explicitly avoiding here.
 */
public class FlashSaleScheduler {

    public interface TickListener {
        void onTick();
    }

    private final List<FlashSale> flashSales;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "FlashSaleScheduler");
        t.setDaemon(true);
        return t;
    });
    private TickListener listener;

    public FlashSaleScheduler(List<FlashSale> flashSales) {
        this.flashSales = flashSales;
    }

    public void setListener(TickListener listener) {
        this.listener = listener;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void tick() {
        for (FlashSale sale : flashSales) {
            sale.refreshStatus();
        }
        if (listener != null) {
            Platform.runLater(listener::onTick);
        }
    }
}
