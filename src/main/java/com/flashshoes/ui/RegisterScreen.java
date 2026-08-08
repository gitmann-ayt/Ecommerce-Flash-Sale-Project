package com.flashshoes.ui;

import com.flashshoes.model.Customer;
import com.flashshoes.service.DataStore;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;

public class RegisterScreen extends StackPane {

    public RegisterScreen() {
        setStyle("-fx-background-color: #F0F4F8;");

        VBox card = new VBox(13);
        card.getStyleClass().add("auth-card");
        card.setMaxWidth(340);
        card.setAlignment(Pos.CENTER_LEFT);

        Label logo = new Label("👟");
        logo.setFont(Font.font(28));
        Label title = new Label("Create your account");
        title.getStyleClass().add("auth-title");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1E3A5F;");
        HBox titleRow = new HBox(8, logo, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Separator sep = new Separator();

        Label nameLbl = new Label("Full name");
        nameLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #374151;");
        TextField nameField = new TextField();
        nameField.setPromptText("John Doe");
        nameField.setMaxWidth(Double.MAX_VALUE);

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
        errorLabel.setMaxWidth(280);

        Button registerBtn = new Button("Create Account");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setDefaultButton(true);
        registerBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String password = passwordField.getText();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                errorLabel.setText("All fields are required.");
                return;
            }
            if (DataStore.getInstance().findUserByEmail(email) != null) {
                errorLabel.setText("An account with that email already exists.");
                return;
            }
            String id = DataStore.getInstance().nextUserId();
            Customer customer = new Customer(id, name, email, password, 500.0);
            DataStore.getInstance().registerCustomer(customer);
            DataStore.getInstance().setCurrentUser(customer);
            MainApp.showCustomerDashboard();
        });

        Hyperlink backLink = new Hyperlink("Already have an account? Log in");
        backLink.setOnAction(e -> MainApp.showLogin());
        backLink.setStyle("-fx-padding: 0;");

        card.getChildren().addAll(
                titleRow, sep,
                nameLbl, nameField,
                emailLbl, emailField,
                pwLbl, passwordField,
                errorLabel, registerBtn, backLink
        );

        setAlignment(card, Pos.CENTER);
        getChildren().add(card);
    }
}
