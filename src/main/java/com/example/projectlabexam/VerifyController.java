package com.example.projectlabexam;

import com.example.projectlabexam.utils.InputSanitizer;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.application.Platform;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class VerifyController {
    @FXML private TextField emailField;
    @FXML private TextField tokenField1;
    @FXML private TextField tokenField2;
    @FXML private TextField tokenField3;
    @FXML private TextField tokenField4;
    @FXML private Label messageLabel;

    private String email;

    public void setEmail(String email) {
        this.email = InputSanitizer.sanitizeEmail(email);
        if (tokenField1 != null) {
            tokenField1.clear();
        }
        if (tokenField2 != null) {
            tokenField2.clear();
        }
        if (tokenField3 != null) {
            tokenField3.clear();
        }
        if (tokenField4 != null) {
            tokenField4.clear();
        }
    }

    @FXML
    public void initialize() {
        if (emailField != null) {
            emailField.setVisible(false);
            emailField.setManaged(false);
        }

        // Add input validation to ensure only digits
        tokenField1.textProperty().addListener(createTokenFieldListener(tokenField1, tokenField2));
        tokenField2.textProperty().addListener(createTokenFieldListener(tokenField2, tokenField3));
        tokenField3.textProperty().addListener(createTokenFieldListener(tokenField3, tokenField4));
        tokenField4.textProperty().addListener(createTokenFieldListener(tokenField4, null));        // Clear error message on input
        tokenField1.setOnKeyTyped(_ -> messageLabel.setText(""));
        tokenField2.setOnKeyTyped(_ -> messageLabel.setText(""));
        tokenField3.setOnKeyTyped(_ -> messageLabel.setText(""));
        tokenField4.setOnKeyTyped(_ -> messageLabel.setText(""));
    }    private ChangeListener<String> createTokenFieldListener(TextField currentField, TextField nextField) {
        return (_, oldValue, newValue) -> {
            // Only allow digits and limit to 1 character
            if (!newValue.matches("\\d") || newValue.length() > 1) {
                currentField.setText(oldValue);
                return;
            }
            
            // Move to next field if available
            if (newValue.length() == 1 && nextField != null) {
                nextField.requestFocus();
            }
        };
    }

    @FXML
    public void handleVerify() {
        if (email == null || email.isEmpty()) {
            email = InputSanitizer.sanitizeEmail(App.getLoggedInCustomerEmail());
        }

        if (email == null || email.isEmpty()) {
            showAlert("Valid email address is required for verification.");
            return;
        }

        // Get and sanitize token
        String token = tokenField1.getText() + tokenField2.getText() + tokenField3.getText() + tokenField4.getText();
        token = InputSanitizer.sanitizeToken(token);
        
        if (token == null || token.length() != 4 || !token.matches("\\d{4}")) {
            showAlert("Please enter a valid 4-digit verification code.");
            return;
        }

        TokenService tokenService = new TokenService();
        boolean success = tokenService.validateToken(email, token);
        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Account Verified");
            alert.setHeaderText(null);
            alert.setContentText("Your email has been verified! Choose your next step.");
            ButtonType goToDashboard = new ButtonType("Go to Customer Dashboard", ButtonBar.ButtonData.OK_DONE);
            ButtonType requestStaff = new ButtonType("Request Staff Account", ButtonBar.ButtonData.OTHER);
            alert.getButtonTypes().setAll(goToDashboard, requestStaff);
            alert.showAndWait().ifPresent(response -> {
                // Update verification status in database
                updateVerificationStatus();
                
                if (response == goToDashboard) {
                    App.switchScene("hoteldashboard.fxml");
                } else if (response == requestStaff) {
                    SignUpController signUpController = new SignUpController();
                    signUpController.handleSendStaffRequest();
                    App.switchScene("login.fxml");
                }
            });
        } else {
            showAlert("Verification failed. Token is invalid or expired. Please request a new token.");
        }
    }

    private void updateVerificationStatus() {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "UPDATE customer SET verified = 1 WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                int rowsUpdated = stmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("Verification status updated successfully for email: " + email);
                } else {
                    System.out.println("No rows updated. Please check if the email exists in the database: " + email);
                    showAlert("Error updating verification status. Please try again or contact support.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error while updating verification status: " + e.getMessage());
            showAlert("Database error occurred. Please try again later.");
        }
    }

    @FXML
    public void handleBackToLogin() {
        App.switchScene("login.fxml");
    }

    @FXML
    public void handleResendCode() {
        if (email == null || email.isEmpty()) {
            email = InputSanitizer.sanitizeEmail(App.getLoggedInCustomerEmail());
        }

        if (email == null || email.isEmpty()) {
            showErrorMessage("Valid email address is required to resend the code.");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (TokenService.getLastResendTime(email) != null) {
            LocalDateTime lastTime = TokenService.getLastResendTime(email);
            if (lastTime.plusSeconds(30).isAfter(now)) {
                showErrorMessage("Please wait at least 30 seconds between resend requests.");
                return;
            }
        }

        TokenService tokenService = new TokenService();
        tokenService.resendValidationCode(email);
        showSuccessMessage("A new validation code has been sent to your email.");
    }

    private void showErrorMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: red;");
        clearMessageAfterDelay();
    }

    private void showSuccessMessage(String message) {
        messageLabel.setText(message);
        messageLabel.setStyle("-fx-text-fill: blue;");
        clearMessageAfterDelay();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void clearMessageAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait for 5 seconds
                Platform.runLater(() -> {
                    messageLabel.setText("");
                    messageLabel.setStyle("");
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
