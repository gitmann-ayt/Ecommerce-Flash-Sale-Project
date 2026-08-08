package com.flashshoes.ui;

import com.flashshoes.model.Admin;
import com.flashshoes.model.User;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginScreen extends StackPane {

    public LoginScreen() {
        setStyle("-fx-background-color: #F0F4F8;");

        // Card
        VBox card = new VBox(14);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(340);
        card.setAlignment(Pos.CENTER_LEFT);

        // Logo / title row
        Label logo = new Label("👟");
        logo.setFont(Font.font(32));
        Label title = new Label("FlashShoes");
        title.getStyleClass().add("auth-title");
        HBox titleRow = new HBox(8, logo, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("Flash sale shoe store — sign in to continue");
        subtitle.getStyleClass().add("auth-subtitle");

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E5E7EB;");

        Label emailLbl = new Label("Email");
        emailLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        TextField emailField = new TextField();
        emailField.setPromptText("you@example.com");
        emailField.setMaxWidth(Double.MAX_VALUE);

        Label pwLbl = new Label("Password");
        pwLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("••••••••");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("auth-error");
        errorLabel.setWrapText(true);

        Button loginBtn = new Button("Log In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setDefaultButton(true);
        loginBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String password = passwordField.getText();
            User user = DataStore.getInstance().findUserByEmail(email);
            if (user == null || !user.login(password)) {
                errorLabel.setText("Invalid email or password.");
                return;
            }
            DataStore.getInstance().setCurrentUser(user);
            if (user instanceof Admin) {
                MainApp.showAdminDashboard();
            } else {
                MainApp.showCustomerDashboard();
            }
        });

        Hyperlink registerLink = new Hyperlink("New here? Create a customer account");
        registerLink.setOnAction(e -> MainApp.showRegister());
        registerLink.setStyle("-fx-padding: 0;");

        Separator sep2 = new Separator();
        Label demoHint = new Label("Demo  admin@flashshoes.com / admin123\nDemo  alex@example.com / customer123");
        demoHint.getStyleClass().add("auth-hint");
        demoHint.setWrapText(true);

        card.getChildren().addAll(
                titleRow, subtitle, sep,
                emailLbl, emailField,
                pwLbl, passwordField,
                errorLabel, loginBtn, registerLink,
                sep2, demoHint
        );

        setAlignment(card, Pos.CENTER);
        getChildren().add(card);
    }
}
