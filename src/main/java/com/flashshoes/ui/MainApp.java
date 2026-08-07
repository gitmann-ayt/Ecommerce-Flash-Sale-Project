package com.flashshoes.ui;

import com.flashshoes.service.DataStore;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    /** Classpath location of the shared theme stylesheet, applied to every screen. */
    private static final String THEME_CSS = "/com/flashshoes/css/app.css";

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        DataStore.getInstance(); // triggers CSV load + background threads on first access

        stage.setTitle("FlashShoes - Flash Sale Shoe Store");
        showLogin();
        stage.show();

        stage.setOnCloseRequest(e -> DataStore.getInstance().shutdown());
    }

    /** Builds a themed Scene: same width/height/root you'd get from `new Scene(...)`, plus app.css applied. */
    private static Scene themedScene(Parent root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(MainApp.class.getResource(THEME_CSS).toExternalForm());
        return scene;
    }

    public static void showLogin() {
        primaryStage.setScene(themedScene(new LoginScreen(), 480, 420));
    }

    public static void showRegister() {
        primaryStage.setScene(themedScene(new RegisterScreen(), 480, 460));
    }

    public static void showCustomerDashboard() {
        primaryStage.setScene(themedScene(new CustomerDashboard(), 1100, 720));
    }

    public static void showAdminDashboard() {
        primaryStage.setScene(themedScene(new AdminDashboard(), 1100, 720));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
