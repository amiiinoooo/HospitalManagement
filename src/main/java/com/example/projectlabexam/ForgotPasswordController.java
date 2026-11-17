package com.example.projectlabexam;

import com.example.projectlabexam.utils.InputSanitizer;
import com.example.projectlabexam.utils.PasswordValidator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ForgotPasswordController {
    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private Label messageLabel;

    private static String savedEmail;    @FXML
    public void handleSendCode() {
        String email = InputSanitizer.sanitizeEmail(emailField.getText());
        if (email == null || email.isEmpty()) {
            messageLabel.setText("Email is required.");
            clearMessageAfterDelay();
            return;
        }

        // Email format validation
        if (!email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
            messageLabel.setText("Please enter a valid email address.");
            clearMessageAfterDelay();
            return;
        }

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT COUNT(*) FROM customer WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        TokenService tokenService = new TokenService();
                        String forgottenPasswordToken = tokenService.generateForgottenPasswordToken(email);
                        EmailService emailService = new EmailService();
                        emailService.sendToken(email, forgottenPasswordToken);
                        savedEmail = email; // Save the email for later use
                        messageLabel.setText("A 4-digit code has been sent to your email.");
                        messageLabel.setStyle("-fx-text-fill: blue;");
                        App.switchScene("forgotpassword_step2.fxml");
                    } else {
                        messageLabel.setText("No account under this email.");
                        messageLabel.setStyle("-fx-text-fill: red;");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("Database error. Please try again later.");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleVerifyCode() {
        String inputToken = codeField.getText();

        if (savedEmail == null || savedEmail.isEmpty() || inputToken == null || inputToken.isEmpty()) {
            messageLabel.setText("Email and code are required.");
            clearMessageAfterDelay();
            return;
        }

        TokenService tokenService = new TokenService();
        if (tokenService.validateForgottenPasswordToken(savedEmail, inputToken)) {
            messageLabel.setText("Code verified successfully.");
            messageLabel.setStyle("-fx-text-fill: green;");
            App.setLoggedInCustomerEmail(savedEmail); // Save email globally
            App.switchScene("forgotpassword_step3.fxml");
        } else {
            messageLabel.setText("Invalid or expired code.");
            messageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleChangePassword() {
        if (savedEmail == null || savedEmail.isEmpty()) {
            messageLabel.setText("Email is required.");
            clearMessageAfterDelay();
            return;
        }

        String newPassword = newPasswordField.getText();
        
        // Add password validation
        PasswordValidator.ValidationResult validationResult = PasswordValidator.validatePassword(newPassword);
        if (!validationResult.isValid()) {
            messageLabel.setText(validationResult.getMessage());
            messageLabel.setStyle("-fx-text-fill: red;");
            clearMessageAfterDelay();
            return;
        }

        try {
            DatabaseUtil.updatePassword(savedEmail, InputSanitizer.sanitizePassword(newPassword));

            // Show popup dialog
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Password Changed");
            alert.setHeaderText(null);
            alert.setContentText("Your password has been changed successfully.");

            ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            alert.getButtonTypes().setAll(okButton);

            alert.showAndWait().ifPresent(response -> {
                if (response == okButton) {
                    App.switchScene("login.fxml");
                }
            });
        } catch (Exception e) {
            messageLabel.setText("Error changing password. Please try again.");
            e.printStackTrace();
        }
        clearMessageAfterDelay();
    }

    @FXML
    public void handleResendCode() {
        if (savedEmail == null || savedEmail.isEmpty()) {
            messageLabel.setText("Email is required to resend the validation code.");
            clearMessageAfterDelay();
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (TokenService.getLastResendTime(savedEmail) != null) {
            LocalDateTime lastTime = TokenService.getLastResendTime(savedEmail);
            if (lastTime.plusSeconds(30).isAfter(now)) {
                messageLabel.setText("You must wait at least 30 seconds before resending the code.");
                clearMessageAfterDelay();
                return;
            }
        }

        TokenService tokenService = new TokenService();
        tokenService.resendForgottenPasswordToken(savedEmail);
        messageLabel.setText("A new validation code has been sent to your email.");
        messageLabel.setStyle("-fx-text-fill: blue;");
        clearMessageAfterDelay();
    }

    private void clearMessageAfterDelay() {
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Wait 5 seconds
                Platform.runLater(() -> messageLabel.setText(""));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
