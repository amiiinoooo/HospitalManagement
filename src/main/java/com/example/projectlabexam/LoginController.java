package com.example.projectlabexam;

import com.example.projectlabexam.utils.InputSanitizer;
import com.example.projectlabexam.utils.PasswordHasher;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LoginController {
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;
    @FXML private Button loginButton;

    // Brute-force protection: track failed attempts and lockout time
    private static class FailedLoginInfo {
        int attempts;
        long lockoutUntil;
    }
    private static final Map<String, FailedLoginInfo> failedLoginMap = new HashMap<>();
    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 10 * 60 * 1000; // 10 minutes

    @FXML
    public void initialize() {
        // Clear message when user types
        emailField.setOnKeyTyped(e -> messageLabel.setText(""));
        passwordField.setOnKeyTyped(e -> messageLabel.setText(""));
    }

    @FXML
    public void handleLogin() {
        String email = InputSanitizer.sanitizeEmail(emailField.getText());
        String password = passwordField.getText();
        String sanitizedPassword = InputSanitizer.sanitizePassword(passwordField.getText());

        if (email == null) {
            messageLabel.setText("Please enter a valid email address.");
            return;
        }

        // Brute-force check
        FailedLoginInfo info = failedLoginMap.getOrDefault(email, new FailedLoginInfo());
        long now = System.currentTimeMillis();
        if (info.lockoutUntil > now) {
            long minutesLeft = (info.lockoutUntil - now) / 60000 + 1;
            messageLabel.setText("Account locked. Try again in " + minutesLeft + " min.");
            return;
        }

        if (sanitizedPassword == null) {
            messageLabel.setText("Password is too short or contains invalid characters.");
            return;
        }

        if (email.isEmpty() || password.isEmpty()) {
            messageLabel.setText("Please enter both email and password.");
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT id, name, password, role, verified FROM customer WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String dbName = rs.getString("name");
                        String dbPassword = rs.getString("password");
                        String dbRole = rs.getString("role");
                        int dbId = rs.getInt("id");
                        boolean isVerified = rs.getBoolean("verified");

                        // Check hashed password
                        if (!PasswordHasher.checkPassword(password, dbPassword)) {
                            // Failed attempt
                            info.attempts++;
                            if (info.attempts >= MAX_ATTEMPTS) {
                                info.lockoutUntil = now + LOCKOUT_DURATION_MS;
                                messageLabel.setText("Too many failed attempts. Try again in 10 min.");
                            } else {
                                messageLabel.setText("Incorrect password. Attempts: " + info.attempts);
                            }
                            failedLoginMap.put(email, info);
                            return;
                        }

                        if (!isVerified) {
                            messageLabel.setText("Account not verified. Please verify your email.");
                            return;
                        }

                        // Login successful: reset failed attempts
                        failedLoginMap.remove(email);
                        messageLabel.setText("Welcome " + dbName + " (" + dbRole + ")!");
                        App.setLoggedInRole(dbRole);
                        if ("Staff".equalsIgnoreCase(dbRole)) {
                            App.setLoggedInStaffName(dbName);
                            App.setLoggedInStaffId(dbId);
                            App.switchScene("staffdashboard.fxml");
                        } else {
                            App.setLoggedInCustomerName(dbName);
                            App.setLoggedInCustomerId(dbId);
                            App.switchScene("hoteldashboard.fxml");
                        }
                    } else {
                        messageLabel.setText("No user found with this email.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database connection error. Please try again later.");
        }
    }

    @FXML
    public void goToSignUp() {
        App.switchScene("signup.fxml");
    }

    @FXML
    public void goToForgotPassword() {
        App.switchScene("forgotpassword_step1.fxml");
    }
}