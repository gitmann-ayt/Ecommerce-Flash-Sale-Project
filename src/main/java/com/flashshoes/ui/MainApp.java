package com.flashshoes.ui;

import com.flashshoes.model.Customer;
import com.flashshoes.model.Product;
import com.flashshoes.service.DataStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        DataStore.getInstance(); // triggers CSV load + background threads on first access

        stage.setTitle("FlashShoes - Flash Sale Shoe Store");
        showLogin();
        stage.show();

        stage.setOnCloseRequest(e -> DataStore.getInstance().shutdown());
    }

    static String css() {
        var res = MainApp.class.getResource("/styles.css");
        return res != null ? res.toExternalForm() : "";
    }

    public static void showLogin() {
        Scene scene = new Scene(new LoginScreen(), 480, 440);
        scene.getStylesheets().add(css());
        primaryStage.setScene(scene);
    }

    public static void showRegister() {
        Scene scene = new Scene(new RegisterScreen(), 480, 480);
        scene.getStylesheets().add(css());
        primaryStage.setScene(scene);
    }

    public static void showCustomerDashboard() {
        Scene scene = new Scene(new CustomerDashboard(), 1100, 720);
        scene.getStylesheets().add(css());
        primaryStage.setScene(scene);
    }

    public static void showAdminDashboard() {
        Scene scene = new Scene(new AdminDashboard(), 1100, 720);
        scene.getStylesheets().add(css());
        primaryStage.setScene(scene);
    }

    public static void showProductDetails(Product product) {
        Scene scene = new Scene(new ProductDetailsScreen(product, MainApp::showCustomerDashboard), 1100, 720);
        scene.getStylesheets().add(css());
        primaryStage.setScene(scene);
    }

    public static void showOrderHistory() {
        Customer customer = (Customer) DataStore.getInstance().getCurrentUser();
        Scene scene = new Scene(new OrderHistoryScreen(customer, MainApp::showCustomerDashboard), 1100, 720);
        scene.getStylesheets().add(css());
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
