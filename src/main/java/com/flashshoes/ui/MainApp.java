package com.flashshoes.ui;

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

    public static void showLogin() {
        primaryStage.setScene(new Scene(new LoginScreen(), 480, 420));
    }

    public static void showRegister() {
        primaryStage.setScene(new Scene(new RegisterScreen(), 480, 460));
    }

    public static void showCustomerDashboard() {
        primaryStage.setScene(new Scene(new CustomerDashboard(), 1100, 720));
    }

    public static void showAdminDashboard() {
        primaryStage.setScene(new Scene(new AdminDashboard(), 1100, 720));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
