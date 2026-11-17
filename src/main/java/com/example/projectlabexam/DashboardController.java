package com.example.projectlabexam;

import com.example.projectlabexam.utils.InputSanitizer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javafx.event.ActionEvent;

public class DashboardController {
    @FXML private ComboBox<String> roomComboBox;
    @FXML private DatePicker checkInDate;
    @FXML private DatePicker checkOutDate;
    @FXML private Label bookingDetails;
    @FXML private Button keycardButton;
    @FXML private Label customerNameLabel;

    // Booking history table fields
    @FXML private TableView<BookingHistory> bookingHistoryTable;
    @FXML private TableColumn<BookingHistory, String> historyRoomColumn;
    @FXML private TableColumn<BookingHistory, String> historyCheckInColumn;
    @FXML private TableColumn<BookingHistory, String> historyCheckOutColumn;
    @FXML private TableColumn<BookingHistory, String> historyStatusColumn;
    @FXML private TableColumn<BookingHistory, Void> historyActionsColumn;
    private javafx.collections.ObservableList<BookingHistory> bookingHistoryList = javafx.collections.FXCollections.observableArrayList();

    private Booking currentBooking;

    @FXML
    public void initialize() {
        // Set the customer name label at the top right
        if (customerNameLabel != null && App.getLoggedInCustomerName() != null) {
            String sanitizedName = InputSanitizer.sanitizeName(App.getLoggedInCustomerName());
            customerNameLabel.setText("Customer: " + sanitizedName);
        }

        // Initialize booking history table
        if (bookingHistoryTable != null) {
            historyRoomColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getRoom()));
            historyCheckInColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCheckIn()));
            historyCheckOutColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCheckOut()));
            historyStatusColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStatus()));

            // Setup delete button column
            historyActionsColumn.setCellFactory(_ -> new TableCell<BookingHistory, Void>() {
                private final Button deleteButton = new Button("Delete");

                {
                    deleteButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
                    deleteButton.setOnAction(_ -> {
                        BookingHistory booking = getTableView().getItems().get(getIndex());
                        if (booking != null && "Reserved".equals(booking.getStatus())) {
                            deleteBooking(booking);
                        } else {
                            showAlert("Cannot Delete", "Only reserved bookings can be deleted.");
                        }
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        BookingHistory booking = getTableView().getItems().get(getIndex());
                        if (booking != null && "Reserved".equals(booking.getStatus())) {
                            setGraphic(deleteButton);
                        } else {
                            setGraphic(null);
                        }
                    }
                }
            });

            bookingHistoryTable.setItems(bookingHistoryList);
            loadBookingHistory();
        }

        // Initialize room combo box
        roomComboBox.getItems().clear();
        try (Connection conn = DatabaseUtil.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT number, roomtype FROM room");
            ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String roomNumber = rs.getString("number");
                String roomType = rs.getString("roomtype");
                if (roomType == null || roomType.isEmpty()) {
                    // fallback to mod logic if not set
                    int num = Integer.parseInt(roomNumber);
                    if (num % 3 == 0) roomType = "Suite";
                    else if (num % 2 == 0) roomType = "Deluxe";
                    else roomType = "Standard";
                }
                roomComboBox.getItems().add(roomNumber + " - " + roomType);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bookingDetails.setText("Failed to load rooms from database.");
        }
    }

    private void loadBookingHistory() {
        bookingHistoryList.clear();
        int customerId = App.getLoggedInCustomerId();
        String sql = "SELECT b.id, r.number, r.roomtype, b.check_in, b.check_out FROM booking b JOIN room r ON b.room_id = r.id WHERE b.customer_id = ? ORDER BY b.check_in DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String room = rs.getString("number") + " - " + rs.getString("roomtype");
                    String checkIn = rs.getDate("check_in").toString();
                    String checkOut = rs.getDate("check_out").toString();
                    String status = getBookingStatus(rs.getDate("check_in").toLocalDate(), rs.getDate("check_out").toLocalDate());
                    int bookingId = rs.getInt("id");
                    bookingHistoryList.add(new BookingHistory(room, checkIn, checkOut, status, bookingId));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getBookingStatus(LocalDate checkIn, LocalDate checkOut) {
        LocalDate today = LocalDate.now();
        if (today.isAfter(checkOut)) return "Completed";
        if (today.isBefore(checkIn)) return "Reserved";
        return "Active";
    }

    @FXML
    private void handleBooking() {
        String selectedRoom = InputSanitizer.sanitizeInput(roomComboBox.getValue());
        LocalDate checkIn = checkInDate.getValue();
        LocalDate checkOut = checkOutDate.getValue();

        // Input validation
        if (selectedRoom == null || checkIn == null || checkOut == null) {
            bookingDetails.setText("Please fill in all fields.");
            return;
        }
        if (checkIn.isBefore(LocalDate.now())) {
            bookingDetails.setText("Check-in date must be today or in the future.");
            return;
        }
        if (!checkIn.isBefore(checkOut)) {
            bookingDetails.setText("Check-in must be before check-out.");
            return;
        }
        if (!isRoomAvailable(selectedRoom, checkIn, checkOut)) {
            String unavailableRange = getUnavailableDateRange(selectedRoom, checkIn, checkOut);
            if (unavailableRange != null) {
                bookingDetails.setText("Room is not available for the selected dates. Unavailable: " + unavailableRange);
            } else {
                bookingDetails.setText("Room is not available for the selected dates.");
            }
            return;
        }

        boolean success = saveBookingToDatabase(selectedRoom, checkIn, checkOut);
        if (success) {
            currentBooking = new Booking(selectedRoom, checkIn, checkOut);
            bookingDetails.setText("Booked " + selectedRoom + " from " + checkIn + " to " + checkOut);
            // Update booking history after successful booking
            loadBookingHistory();
        } else {
            bookingDetails.setText("Failed to book room. Please try again.");
        }
        updateKeycardStatus();
    }

    // Check if the room is available for the selected dates
    private boolean isRoomAvailable(String room, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String roomNumber = room.split(" - ")[0];
            int roomId = -1;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM room WHERE number = ?")) {
                stmt.setString(1, roomNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        roomId = rs.getInt("id");
                    }
                }
            }
            if (roomId == -1) return false;
            String sql = "SELECT COUNT(*) FROM booking WHERE room_id = ? AND (check_in < ? AND check_out > ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomId);
                stmt.setDate(2, java.sql.Date.valueOf(checkOut));
                stmt.setDate(3, java.sql.Date.valueOf(checkIn));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) == 0;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean saveBookingToDatabase(String room, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String roomNumber = room.split(" - ")[0];
            int roomId = -1;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM room WHERE number = ?")) {
                stmt.setString(1, roomNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        roomId = rs.getInt("id");
                    }
                }
            }
            if (roomId == -1) return false;

            // For demo, use a default customer ID (e.g., 1)
            int customerId = App.getLoggedInCustomerId();

            String sql = "INSERT INTO booking (customer_id, room_id, check_in, check_out) VALUES (?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, customerId);
                stmt.setInt(2, roomId);
                stmt.setDate(3, java.sql.Date.valueOf(checkIn));
                stmt.setDate(4, java.sql.Date.valueOf(checkOut));
                int rows = stmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void handleKeycardUse() {
        if (currentBooking != null && currentBooking.isValid()) {
            bookingDetails.setText("Keycard access granted for " + currentBooking.getRoom());
        } else {
            bookingDetails.setText("No valid booking for keycard access.");
        }
    }
    public void switchToStaff(ActionEvent event) {
        App.switchScene("staffdashboard.fxml");
    }
    private void updateKeycardStatus() {
        keycardButton.setDisable(currentBooking == null || !currentBooking.isValid());
    }

    @FXML
    private void backToLogin() {
        App.signOut();
        App.switchScene("login.fxml");
    }

    public void setCustomerName(String name) {
        if (customerNameLabel != null) {
            String sanitizedName = InputSanitizer.sanitizeName(name);
            customerNameLabel.setText("Customer: " + sanitizedName);
        }
    }

    private String getUnavailableDateRange(String room, LocalDate checkIn, LocalDate checkOut) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String roomNumber = room.split(" - ")[0];
            int roomId = -1;
            try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM room WHERE number = ?")) {
                stmt.setString(1, roomNumber);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        roomId = rs.getInt("id");
                    }
                }
            }
            if (roomId == -1) return null;
            String sql = "SELECT check_in, check_out FROM booking WHERE room_id = ? AND (check_in < ? AND check_out > ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, roomId);
                stmt.setDate(2, java.sql.Date.valueOf(checkOut));
                stmt.setDate(3, java.sql.Date.valueOf(checkIn));
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        LocalDate unavailableFrom = rs.getDate("check_in").toLocalDate();
                        LocalDate unavailableTo = rs.getDate("check_out").toLocalDate();
                        return unavailableFrom + " to " + unavailableTo;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteBooking(BookingHistory booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to delete this booking?\nRoom: " + booking.getRoom() +
                "\nCheck-in: " + booking.getCheckIn() +
                "\nCheck-out: " + booking.getCheckOut(),
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Delete Booking");
        confirm.setHeaderText(null);
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                if (deleteBookingFromDatabase(booking)) {
                    bookingHistoryList.remove(booking);
                    showAlert("Success", "Booking deleted successfully.");
                } else {
                    showAlert("Error", "Failed to delete booking. Please try again.");
                }
            }
        });
    }    private boolean deleteBookingFromDatabase(BookingHistory booking) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            String sql = "DELETE FROM booking WHERE id = ? AND customer_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, booking.getBookingId());
                stmt.setInt(2, App.getLoggedInCustomerId());
                int deleted = stmt.executeUpdate();
                return deleted > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class BookingHistory {
        private final String room;
        private final String checkIn;
        private final String checkOut;
        private final String status;
        private final int bookingId;

        public BookingHistory(String room, String checkIn, String checkOut, String status, int bookingId) {
            this.room = room;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.status = status;
            this.bookingId = bookingId;
        }

        public String getRoom() { return room; }
        public String getCheckIn() { return checkIn; }
        public String getCheckOut() { return checkOut; }
        public String getStatus() { return status; }
        public int getBookingId() { return bookingId; }
    }
}