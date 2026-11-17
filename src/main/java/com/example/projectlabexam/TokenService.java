package com.example.projectlabexam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class TokenService {

    private final int EXPIRATION_MINUTES = 5;
    private static final Map<String, LocalDateTime> lastResendTime = new HashMap<>();

    public String generateToken(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty.");
        }

        String token = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO tokens (email, token, expiry) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE token = ?, expiry = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.setString(2, token);
                stmt.setTimestamp(3, java.sql.Timestamp.valueOf(expiryTime));
                stmt.setString(4, token);
                stmt.setTimestamp(5, java.sql.Timestamp.valueOf(expiryTime));
                stmt.executeUpdate();
                System.out.println("Token stored in database: " + token + " for email: " + email);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return token;
    }

    public boolean validateToken(String email, String inputToken) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT token, expiry FROM tokens WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedToken = rs.getString("token");
                        LocalDateTime expiry = rs.getTimestamp("expiry").toLocalDateTime();
                        System.out.println("Retrieved token: " + storedToken + ", Input token: " + inputToken + ", Expiry: " + expiry);
                        if (expiry.isBefore(LocalDateTime.now())) {
                            System.out.println("Token expired.");
                            return false;
                        }
                        boolean isValid = storedToken.equals(inputToken);
                        System.out.println("Token validation result: " + isValid);
                        if (isValid) {
                            // Update the verified column in the database
                            String updateSql = "UPDATE customer SET verified = 1 WHERE email = ?";
                            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                                updateStmt.setString(1, email);
                                int rowsUpdated = updateStmt.executeUpdate();
                                if (rowsUpdated > 0) {
                                    System.out.println("Verification status updated successfully for email: " + email);
                                } else {
                                    System.out.println("No rows updated. Please check if the email exists in the database: " + email);
                                }
                            }
                        }
                        return isValid;
                    } else {
                        System.out.println("No token found for email: " + email);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void clearToken(String email) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "DELETE FROM tokens WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resendValidationCode(String email) {
        LocalDateTime now = LocalDateTime.now();
        if (lastResendTime.containsKey(email)) {
            LocalDateTime lastTime = lastResendTime.get(email);
            if (lastTime.plusSeconds(30).isAfter(now)) {
                System.out.println("You must wait at least 30 seconds before resending the code.");
                return;
            }
        }

        String newToken = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime newExpiryTime = now.plusMinutes(EXPIRATION_MINUTES);

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "UPDATE tokens SET token = ?, expiry = ? WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newToken);
                stmt.setTimestamp(2, java.sql.Timestamp.valueOf(newExpiryTime));
                stmt.setString(3, email);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    System.out.println("New token generated and updated in database: " + newToken);
                    // Send the new token to the user via email
                    EmailService emailService = new EmailService();
                    emailService.sendToken(email, newToken);
                    System.out.println("New token sent to email: " + email);
                    lastResendTime.put(email, now);
                } else {
                    System.out.println("Failed to update token. Email not found: " + email);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static LocalDateTime getLastResendTime(String email) {
        return lastResendTime.get(email);
    }

    public static String generateVerificationCode() {
        return String.format("%04d", (int) (Math.random() * 10000));
    }

    public static void sendVerificationCode(String email, String code) {
        EmailService emailService = new EmailService();
        emailService.sendToken(email, code);
    }

    public static void updatePassword(String email, String newPassword) throws SQLException {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "UPDATE customer SET password = ? WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newPassword);
                stmt.setString(2, email);
                stmt.executeUpdate();
            }
        }
    }

    public String generateForgottenPasswordToken(String email) {
        String token = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "INSERT INTO ForgottenPasswordToken (email, token, expiry) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE token = ?, expiry = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.setString(2, token);
                stmt.setTimestamp(3, java.sql.Timestamp.valueOf(expiryTime));
                stmt.setString(4, token);
                stmt.setTimestamp(5, java.sql.Timestamp.valueOf(expiryTime));
                int rowsAffected = stmt.executeUpdate();
                System.out.println("SQL executed. Rows affected: " + rowsAffected);
                System.out.println("Forgotten password token stored in database: " + token + " for email: " + email);
            }
        } catch (SQLException e) {
            System.err.println("Error executing SQL for forgotten password token: " + e.getMessage());
            e.printStackTrace();
        }

        return token;
    }

    public boolean validateForgottenPasswordToken(String email, String inputToken) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "SELECT token, expiry FROM ForgottenPasswordToken WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedToken = rs.getString("token");
                        LocalDateTime expiry = rs.getTimestamp("expiry").toLocalDateTime();
                        System.out.println("Retrieved forgotten password token: " + storedToken + ", Input token: " + inputToken + ", Expiry: " + expiry);
                        if (expiry.isBefore(LocalDateTime.now())) {
                            System.out.println("Forgotten password token expired.");
                            return false;
                        }
                        boolean isValid = storedToken.equals(inputToken);
                        System.out.println("Forgotten password token validation result: " + isValid);
                        return isValid;
                    } else {
                        System.out.println("No forgotten password token found for email: " + email);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void clearForgottenPasswordToken(String email) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "DELETE FROM ForgottenPasswordToken WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, email);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resendForgottenPasswordToken(String email) {
        LocalDateTime now = LocalDateTime.now();
        if (lastResendTime.containsKey(email)) {
            LocalDateTime lastTime = lastResendTime.get(email);
            if (lastTime.plusSeconds(30).isAfter(now)) {
                System.out.println("You must wait at least 30 seconds before resending the code.");
                return;
            }
        }

        String newToken = String.format("%04d", new Random().nextInt(10000));
        LocalDateTime newExpiryTime = now.plusMinutes(EXPIRATION_MINUTES);

        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "UPDATE forgottenpasswordtoken SET token = ?, expiry = ? WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newToken);
                stmt.setTimestamp(2, java.sql.Timestamp.valueOf(newExpiryTime));
                stmt.setString(3, email);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    System.out.println("New forgotten password token generated and updated in database: " + newToken);
                    EmailService emailService = new EmailService();
                    emailService.sendToken(email, newToken);
                    System.out.println("New forgotten password token sent to email: " + email);
                    lastResendTime.put(email, now);
                } else {
                    System.out.println("Failed to update forgotten password token. Email not found: " + email);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
