package com.example.projectlabexam;

import com.example.projectlabexam.utils.InputSanitizer;
import com.example.projectlabexam.utils.PasswordValidator;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SignUpController {
    @FXML private TextField nameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML
    public void initialize() {
        // Clear error message when user types
        nameField.setOnKeyTyped(e -> clearMessage());
        lastNameField.setOnKeyTyped(e -> clearMessage());
        emailField.setOnKeyTyped(e -> clearMessage());
        passwordField.setOnKeyTyped(e -> clearMessage());

        // Limit nameField to 25 characters
        nameField.textProperty().addListener((_, oldText, newText) -> {
            if (newText != null && newText.length() > 25) {
                nameField.setText(oldText);
            }
        });

        lastNameField.textProperty().addListener((_, oldText, newText) -> {
            if (newText != null && newText.length() > 25) {
                lastNameField.setText(oldText);
            }
        });
    }

    private void clearMessage() {
        messageLabel.setText("");
        messageLabel.setStyle("");
    }

    @FXML
    public void handleSignUp() {
        System.out.println("SignUp button clicked"); // Debug output
        String name = InputSanitizer.sanitizeName(nameField.getText());
        String lastName = InputSanitizer.sanitizeName(lastNameField.getText());
        String email = InputSanitizer.sanitizeEmail(emailField.getText());
        String rawPassword = passwordField.getText();

        // Check for null values (invalid input)
        if (name == null || lastName == null) {
            messageLabel.setText("Names must contain only letters, hyphens, and spaces.");
            return;
        }

        if (email == null) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }

        if (rawPassword == null) {
            messageLabel.setText("Password is too short or contains invalid characters.");
            return;
        }

        if (name.isEmpty() || lastName.isEmpty() || email.isEmpty() || rawPassword.isEmpty()) {
            messageLabel.setText("Please fill all fields.");
            return;
        }

        // Name format validation (first letter must be uppercase)
        if (!isValidName(name) || !isValidName(lastName)) {
            messageLabel.setText("Names must start with uppercase letters (e.g., 'John').");
            return;
        }

        // Password validation using PasswordValidator
        PasswordValidator.ValidationResult validationResult = PasswordValidator.validatePassword(rawPassword);
        if (!validationResult.isValid()) {
            messageLabel.setText(validationResult.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        String password = InputSanitizer.sanitizePassword(rawPassword); // Hash after validation

        try (Connection conn = DatabaseUtil.getConnection()) {
            // Check if email already exists
            String checkSql = "SELECT id FROM customer WHERE email = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, email);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        messageLabel.setText("Email already registered.");
                        messageLabel.setStyle("-fx-text-fill: red;");
                        return;
                    }
                }
            }

            // Generate a verification token
            TokenService tokenService = new TokenService();
            String verificationToken = tokenService.generateToken(email);

            // Insert new customer with verification token
            String insertSql = "INSERT INTO customer (name, email, password, role, verified, verification_token) VALUES (?, ?, ?, 'Customer', 0, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, name + " " + lastName);
                insertStmt.setString(2, email);
                insertStmt.setString(3, password);
                insertStmt.setString(4, verificationToken);

                int rows = insertStmt.executeUpdate();
                if (rows > 0) {
                    // Retrieve the newly created customer ID
                    int customerId = -1;
                    try (PreparedStatement getIdStmt = conn.prepareStatement("SELECT id FROM customer WHERE email = ?")) {
                        getIdStmt.setString(1, email);
                        try (ResultSet rs = getIdStmt.executeQuery()) {
                            if (rs.next()) {
                                customerId = rs.getInt("id");
                            }
                        }
                    }

                    if (customerId != -1) {
                        // Set logged-in user details
                        App.setLoggedInCustomerId(customerId);
                        App.setLoggedInCustomerName(name + " " + lastName);
                        App.setLoggedInCustomerEmail(email);
                    }

                    // Send verification token
                    EmailService emailService = new EmailService();
                    emailService.sendToken(email, verificationToken);

                    // Show success popup
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Account Created");
                    alert.setHeaderText(null);
                    alert.setContentText("Account successfully created. Please verify your email.");
                    ButtonType verifyEmailButton = new ButtonType("Verify Email", ButtonBar.ButtonData.OK_DONE);
                    alert.getButtonTypes().setAll(verifyEmailButton);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == verifyEmailButton) {
                            // Redirect to token verification screen
                            App.switchScene("verify.fxml");
                        }
                    });

                    return;
                } else {
                    messageLabel.setText("Sign up failed. Try again.");
                    messageLabel.setStyle("-fx-text-fill: red;");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error: " + e.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void goToLogin() {
        App.switchScene("login.fxml");
    }

    // Helper method to validate name format
    private boolean isValidName(String input) {
        // Allow names with spaces, hyphens, and apostrophes, each word must start with uppercase
        String[] words = input.trim().split("[ \\-']+"); // space, hyphen, apostrophe
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!word.matches("[A-Z][a-z]*")) {
                return false;
            }
        }
        // Allow spaces, hyphens, and apostrophes in the name
        return input.matches("[A-Za-z \\-']{2,50}");
    }

    // Handle staff account requests
    public void handleSendStaffRequest() {
        int customerId = App.getLoggedInCustomerId();
        String customerName = App.getLoggedInCustomerName();

        if (customerId == 0 || customerName == null) {
            showAlert("Error", "You must be logged in to send a staff account request.");
            return;
        }

        try (Connection connection = DatabaseUtil.getConnection()) {
            // Check if request already exists
            String checkSql = "SELECT COUNT(*) FROM staff_requests WHERE customer_id = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setInt(1, customerId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        showAlert("Error", "You have already submitted a staff account request.");
                        return;
                    }
                }
            }

            // Insert new request
            String sql = "INSERT INTO staff_requests (customer_id, customer_name, requested_at) VALUES (?, ?, NOW())";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, customerId);
                statement.setString(2, customerName);
                statement.executeUpdate();
                showAlert("Success", "Your staff account request has been sent successfully!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Failed to send the staff account request. Please try again.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
