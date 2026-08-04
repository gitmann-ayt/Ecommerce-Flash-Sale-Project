package com.flashshoes.ui;

import com.flashshoes.model.Customer;
import com.flashshoes.service.DataStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class RegisterScreen extends VBox {

    public RegisterScreen() {
        setSpacing(12);
        setPadding(new Insets(40));
        setAlignment(Pos.CENTER);

        Label title = new Label("Create your account");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        TextField nameField = new TextField();
        nameField.setPromptText("Full name");
        nameField.setMaxWidth(280);

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        emailField.setMaxWidth(280);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setMaxWidth(280);

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #c0392b;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(280);

        Button registerBtn = new Button("Create Account");
        registerBtn.setMaxWidth(280);
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
            Customer customer = new Customer(id, name, email, password, 500.0); // starter wallet balance for demo
            DataStore.getInstance().registerCustomer(customer);
            DataStore.getInstance().setCurrentUser(customer);
            MainApp.showCustomerDashboard();
        });

        Hyperlink backLink = new Hyperlink("Already have an account? Log in");
        backLink.setOnAction(e -> MainApp.showLogin());

        getChildren().addAll(title, nameField, emailField, passwordField, errorLabel, registerBtn, backLink);
    }
}
