package com.flashshoes.ui;

import com.flashshoes.model.Admin;
import com.flashshoes.model.User;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginScreen extends VBox {

    public LoginScreen() {
        setSpacing(14);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        Label title = new Label("FlashShoes");
        title.setFont(Font.font("System", FontWeight.BOLD, 28));

        Label subtitle = new Label("Flash sale shoe store - sign in to continue");
        subtitle.setStyle("-fx-text-fill: #666;");

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setMaxWidth(280);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(280);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #c0392b;");

        Button loginBtn = new Button("Log In");
        loginBtn.setMaxWidth(280);
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

        Label demoHint = new Label("Demo admin: admin@flashshoes.com / admin123\nDemo customer: alex@example.com / customer123");
        demoHint.setStyle("-fx-text-fill: #999; -fx-font-size: 10;");
        demoHint.setWrapText(true);
        demoHint.setAlignment(Pos.CENTER);

        getChildren().addAll(title, subtitle, emailField, passwordField, errorLabel, loginBtn, registerLink, demoHint);
    }
}
