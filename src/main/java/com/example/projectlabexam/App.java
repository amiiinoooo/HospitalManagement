package com.example.projectlabexam;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class App extends Application {

    private static Stage primaryStage;
    private static Scene scene;
    private static String loggedInStaffName;
    private static String loggedInCustomerName;
    private static String loggedInCustomerEmail;
    private static int loggedInCustomerId;
    private static int loggedInStaffId;
    private static String loggedInRole;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        switchScene("login.fxml");
        primaryStage.show();
    }

    public static void setLoggedInStaffName(String name) {
        loggedInStaffName = name;
    }

    public static String getLoggedInStaffName() {
        return loggedInStaffName;
    }

    public static void setLoggedInCustomerName(String name) {
        loggedInCustomerName = name;
    }

    public static String getLoggedInCustomerName() {
        return loggedInCustomerName;
    }

    public static void setLoggedInCustomerEmail(String email) {
        loggedInCustomerEmail = email;
    }

    public static String getLoggedInCustomerEmail() {
        return loggedInCustomerEmail;
    }

    public static void setLoggedInCustomerId(int id) {
        loggedInCustomerId = id;
    }

    public static int getLoggedInCustomerId() {
        return loggedInCustomerId;
    }

    public static void setLoggedInStaffId(int id) {
        loggedInStaffId = id;
    }

    public static int getLoggedInStaffId() {
        return loggedInStaffId;
    }

    public static void setLoggedInRole(String role) {
        loggedInRole = role;
    }

    public static String getLoggedInRole() {
        return loggedInRole;
    }

    public static void switchScene(String fxml) {
        try {
            System.out.println("Attempting to switch to scene: " + fxml);
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
            Parent root = loader.load();
            if (scene == null) {
                scene = new Scene(root);
                primaryStage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
            primaryStage.setWidth(900);
            primaryStage.setHeight(700);
            primaryStage.setResizable(true);
            System.out.println("Scene switched successfully to: " + fxml);
        } catch (IOException e) {
            System.err.println("Failed to switch scene to: " + fxml);
            e.printStackTrace();
        }
    }

    public static void signOut() {
        loggedInStaffName = null;
        loggedInCustomerName = null;
        loggedInCustomerEmail = null;
        loggedInRole = null;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // No need to close a shared connection; each DB operation manages its own connection.
    }

    public static void main(String[] args) {
        DatabaseUtil.testConnection(); // Test the database connection
        launch();

        // Example usage of TokenService and EmailService
        TokenService tokenService = new TokenService();
        EmailService emailService = new EmailService();

        // When user signs up or clicks "resend"
        String userEmail = App.getLoggedInCustomerName(); // Fetch the logged-in user's email dynamically
        String token = tokenService.generateToken(userEmail);
        emailService.sendToken(userEmail, token);

        // When verifying
        String userInputToken = "1234"; // Replace with actual user input
        if (tokenService.validateToken(userEmail, userInputToken)) {
            System.out.println("Token valid. Proceed with verification.");
            tokenService.clearToken(userEmail); // Optional
        } else {
            System.out.println("Invalid or expired token.");
        }
    }
}
