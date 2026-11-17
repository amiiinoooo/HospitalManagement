package com.example.projectlabexam;

import com.example.projectlabexam.utils.InputSanitizer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.event.ActionEvent;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class StaffDashboardController {

    // Room View Tab
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, String> roomNumberColumn;
    @FXML private TableColumn<Room, String> roomTypeColumn;
    @FXML private TableColumn<Room, String> statusColumn;
    @FXML private TableColumn<Room, String> lastCleanedColumn;
    @FXML private TableColumn<Room, Void> actionsColumn;
    @FXML private Label availableCountLabel;
    @FXML private Label occupiedCountLabel;
    @FXML private Label cleaningCountLabel;

    // Booking View Tab
    @FXML private TableView<Booking> bookingTable;
    @FXML private TableColumn<Booking, String> bookingIdColumn;
    @FXML private TableColumn<Booking, String> customerNameColumn;
    @FXML private TableColumn<Booking, String> bookingRoomColumn;
    @FXML private TableColumn<Booking, String> checkInColumn;
    @FXML private TableColumn<Booking, String> checkOutColumn;
    @FXML private TableColumn<Booking, String> bookingStatusColumn;
    @FXML private TableColumn<Booking, Void> bookingActionsColumn;
    @FXML private TextField bookingSearchField;

    // Update Room Status Tab
    @FXML private ComboBox<String> roomSelect;
    @FXML private Label currentStatusLabel;
    @FXML private Button btnAvailable;
    @FXML private Button btnOccupied;
    @FXML private Button btnCleaning;
    @FXML private Button updateStatusButton;

    // Access Logs Tab
    @FXML private TableView<AccessLog> accessLogTable;
    @FXML private TableColumn<AccessLog, String> timestampColumn;
    @FXML private TableColumn<AccessLog, String> logRoomColumn;
    @FXML private TableColumn<AccessLog, String> accessTypeColumn;
    @FXML private TableColumn<AccessLog, String> logCustomerColumn;
    @FXML private TableColumn<AccessLog, String> detailsColumn;
    @FXML private DatePicker logDateFilter;
    @FXML private Button applyFilterButton;

    // Requests Tab
    @FXML private TableView<Request> requestsTable;
    @FXML private TableColumn<Request, String> requestNameColumn;
    @FXML private TableColumn<Request, String> requestEmailColumn;
    @FXML private TableColumn<Request, String> requestDateColumn;
    @FXML private TableColumn<Request, Void> requestActionsColumn;

    // Staff name label
    @FXML private Label staffNameLabel;

    // Data collections
    private final ObservableList<Room> allRooms = FXCollections.observableArrayList();
    private final ObservableList<Booking> allBookings = FXCollections.observableArrayList();
    private final ObservableList<AccessLog> allAccessLogs = FXCollections.observableArrayList();
    private final ObservableList<Request> allRequests = FXCollections.observableArrayList();

    // Declare room status counters as class-level variables
    private int availableCount = 0;
    private int occupiedCount = 0;
    private int cleaningCount = 0;

    @FXML
    public void initialize() {
        // Always set staff name label from App static variable
        if (staffNameLabel != null && App.getLoggedInStaffName() != null) {
            String sanitizedName = InputSanitizer.sanitizeName(App.getLoggedInStaffName());
            staffNameLabel.setText("Staff: " + sanitizedName);
        }
        System.out.println("Connected to database!");

        // Initialize all components
        setupRoomsTab();
        setupBookingsTab();
        setupUpdateStatusTab();
        setupAccessLogsTab();
        setupRequestsTab();

        // Load data from the database
        loadDataFromDatabase();
    }

    public void switchToHotel(ActionEvent event) {
        App.switchScene("hoteldashboard.fxml");
    }

    @FXML
    private void backToLogin() {
        App.signOut();
        App.switchScene("login.fxml");
    }

    private void setupRoomsTab() {
        // Set up room table columns
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        roomTypeColumn.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        lastCleanedColumn.setCellValueFactory(new PropertyValueFactory<>("lastCleaned"));

        // Custom status cell styling
        statusColumn.setCellFactory(_ -> new TableCell<Room, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Available":
                            setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
                            break;
                        case "Occupied":
                            setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-font-weight: bold;");
                            break;
                        case "Cleaning":
                            setStyle("-fx-background-color: #fff8e1; -fx-text-fill: #f57c00; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });

        // Setup actions column with buttons
        actionsColumn.setCellFactory(createActionButtonCellFactory(
                (Room room) -> {
                    showAlert("Room Details", "Viewing details for Room " + room.getRoomNumber());
                },
                "Details",
                "-fx-background-color: #3498db; -fx-text-fill: white;"
        ));

        roomTable.setItems(allRooms);
    }

    private void setupBookingsTab() {
        // Set up booking table columns
        bookingIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        customerNameColumn.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        bookingRoomColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        checkInColumn.setCellValueFactory(new PropertyValueFactory<>("checkIn"));
        checkOutColumn.setCellValueFactory(new PropertyValueFactory<>("checkOut"));
        bookingStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Custom booking status cell styling
        bookingStatusColumn.setCellFactory(_ -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (item == null || empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "Active":
                            setStyle("-fx-background-color: #e3f2fd; -fx-text-fill: #0d47a1; -fx-font-weight: bold;");
                            break;
                        case "Reserved":
                            setStyle("-fx-background-color: #e0f2f1; -fx-text-fill: #004d40; -fx-font-weight: bold;");
                            break;
                        case "Completed":
                            setStyle("-fx-background-color: #f3e5f5; -fx-text-fill: #4a148c; -fx-font-weight: bold;");
                            break;
                        default:
                            setStyle("");
                            break;
                    }
                }
            }
        });

        // Setup actions column for bookings
        bookingActionsColumn.setCellFactory(createActionButtonCellFactory(
                (Booking booking) -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this booking?", ButtonType.YES, ButtonType.NO);
                    confirm.setTitle("Delete Booking");
                    confirm.setHeaderText(null);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            try (Connection conn = DatabaseUtil.getConnection()) {
                                String deleteSql = "DELETE FROM booking WHERE id = ?";
                                try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                                    int bookingId = Integer.parseInt(booking.getBookingId().substring(1));
                                    stmt.setInt(1, bookingId);
                                    int deleted = stmt.executeUpdate();
                                    if (deleted > 0) {
                                        allBookings.remove(booking);
                                        showAlert("Success", "Booking deleted successfully.");
                                        // Log this action
                                        String staffName = staffNameLabel != null ? staffNameLabel.getText().replace("Staff: ", "") : "Staff";
                                        String logDetails = booking.getCustomerName() + ": " + booking.getCheckIn().split(" ")[0] + " - " + booking.getCheckOut().split(" ")[0];
                                        addAccessLog(booking.getRoomNumber(), "Booking Deleted", staffName, logDetails);
                                    } else {
                                        showAlert("Error", "Failed to delete booking.");
                                    }
                                }
                            } catch (Exception e) {
                                showAlert("Database Error", "Failed to delete booking: " + e.getMessage());
                            }
                        }
                    });
                },
                "Delete",
                "-fx-background-color: #e74c3c; -fx-text-fill: white;"
        ));        // Initialize the table with all bookings
        bookingTable.setItems(allBookings);
        System.out.println("Initial booking table items set: " + allBookings.size());

        // Setup search functionality
        if (bookingSearchField != null) {
            bookingSearchField.setPromptText("Search by name or booking ID...");
            bookingSearchField.textProperty().addListener((_, _, newValue) -> filterBookings(newValue));
        }
    }

    private void setupUpdateStatusTab() {
        // Initialize room selection combo box (will be populated after loading rooms from DB)
        roomSelect.setOnAction(_ -> {
            String selected = roomSelect.getValue();
            if (selected != null) {
                String roomNumber = selected.split(" - ")[0];

                // Find the room's current status
                for (Room room : allRooms) {
                    if (room.getRoomNumber().equals(roomNumber)) {
                        currentStatusLabel.setText("Current Status: " + room.getStatus());

                        // Set style for status label
                        switch (room.getStatus()) {
                            case "Available":
                                currentStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
                                break;
                            case "Occupied":
                                currentStatusLabel.setStyle("-fx-text-fill: #c62828;");
                                break;
                            case "Cleaning":
                                currentStatusLabel.setStyle("-fx-text-fill: #f57c00;");
                                break;
                        }
                        break;
                    }
                }
            }
        });

        // Setup button actions
        btnAvailable.setOnAction(_ -> prepareStatusUpdate("Available"));
        btnOccupied.setOnAction(_ -> prepareStatusUpdate("Occupied"));
        btnCleaning.setOnAction(_ -> prepareStatusUpdate("Cleaning"));

        // Main update button
        updateStatusButton.setOnAction(_ -> {
            if (roomSelect.getValue() == null) {
                showAlert("Error", "Please select a room first");
                return;
            }

            String roomNumber = roomSelect.getValue().split(" - ")[0];
            String newStatus = getCurrentSelectedStatus();

            // Prevent updating to the same status
            Room selectedRoom = null;
            for (Room room : allRooms) {
                if (room.getRoomNumber().equals(roomNumber)) {
                    selectedRoom = room;
                    break;
                }
            }
            if (selectedRoom == null) {
                showAlert("Error", "Room not found.");
                return;
            }
            if (selectedRoom.getStatus().equals(newStatus)) {
                showAlert("Error", "Room is already in this status");
                return;
            }

            try {
                updateRoomStatus(roomNumber, newStatus);
                // Clear form
                showAlert("Success", "Room " + roomNumber + " status updated to " + newStatus);
                // Log this action with actual staff name
                String staffName = staffNameLabel != null ? staffNameLabel.getText().replace("Staff: ", "") : "Staff";
                addAccessLog(roomNumber, "Status Change", staffName,
                        "Status changed to " + newStatus);
            } catch (Exception ex) {
                showAlert("Error", "Failed to update room status: " + ex.getMessage());
            }
        });
    }

    private void setupAccessLogsTab() {
        // Set up access log table columns
        timestampColumn.setCellValueFactory(cellData -> {
            String original = cellData.getValue().getTimestamp();
            try {
                // Try parsing with both possible formats
                java.time.LocalDateTime dt;
                if (original.matches("\\d{4}/\\d{2}/\\d{2}.*")) {
                    // If format is YYYY/MM/DD, parse and reformat
                    dt = java.time.LocalDateTime.parse(original, java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"));
                } else {
                    // Otherwise, assume DD/MM/YYYY
                    dt = java.time.LocalDateTime.parse(original, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                }
                return new javafx.beans.property.SimpleStringProperty(dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            } catch (Exception e) {
                // If parsing fails, try to parse as LocalDate (date only)
                try {
                    java.time.LocalDate d;
                    if (original.matches("\\d{4}/\\d{2}/\\d{2}")) {
                        d = java.time.LocalDate.parse(original, java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                    } else {
                        d = java.time.LocalDate.parse(original, java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    }
                    return new javafx.beans.property.SimpleStringProperty(d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                } catch (Exception ex) {
                    return new javafx.beans.property.SimpleStringProperty(original);
                }
            }
        });
        logRoomColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        accessTypeColumn.setCellValueFactory(new PropertyValueFactory<>("accessType"));
        logCustomerColumn.setCellValueFactory(new PropertyValueFactory<>("person"));
        detailsColumn.setCellValueFactory(new PropertyValueFactory<>("details"));

        detailsColumn.setCellFactory(_ -> new TableCell<AccessLog, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    // Replace all YYYY-MM-DD with DD/MM/YYYY in the details string
                    String formatted = item.replaceAll(
                        "(\\d{4})-(\\d{2})-(\\d{2})",
                        "$3/$2/$1"
                    );
                    setText(formatted);
                }
            }
        });

        accessLogTable.setItems(allAccessLogs);

        // Setup date filter
        if (logDateFilter != null && applyFilterButton != null) {
            logDateFilter.setValue(LocalDate.now());

            applyFilterButton.setOnAction(_ -> {
                filterAccessLogs(logDateFilter.getValue());
            });
        }
    }

    private void setupRequestsTab() {
        requestNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        requestEmailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        requestDateColumn.setCellValueFactory(cellData -> {
            String original = cellData.getValue().getRequestedAt();
            try {
                java.time.LocalDateTime dt = java.time.LocalDateTime.parse(original);
                return new javafx.beans.property.SimpleStringProperty(dt.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty(original);
            }
        });
        requestActionsColumn.setCellFactory(getRequestActionsCellFactory());
        requestsTable.setItems(allRequests);
        loadRequestsFromDatabase();
    }

    private Callback<TableColumn<Request, Void>, TableCell<Request, Void>> getRequestActionsCellFactory() {
        return new Callback<>() {
            @Override
            public TableCell<Request, Void> call(final TableColumn<Request, Void> param) {
                return new TableCell<>() {
                    private final Button acceptBtn = new Button("Accept");
                    private final Button declineBtn = new Button("Decline");
                    {
                        acceptBtn.setOnAction(_ -> handleAcceptRequest(getTableView().getItems().get(getIndex())));
                        declineBtn.setOnAction(_ -> handleDeclineRequest(getTableView().getItems().get(getIndex())));
                    }
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox box = new HBox(5, acceptBtn, declineBtn);
                            setGraphic(box);
                        }
                    }
                };
            }
        };
    }

    private void loadDataFromDatabase() {
        loadRoomsFromDatabase();
        loadBookingsFromDatabase();
        loadAccessLogsFromDatabase();
        populateRoomSelectComboBox();
        loadRequestsFromDatabase();
    }

    private void loadRoomsFromDatabase() {
        String query = "SELECT r.id, r.number, r.status, r.roomtype FROM room r";
        int availableCount = 0;
        int occupiedCount = 0;
        int cleaningCount = 0;
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            allRooms.clear();
            // Reset counts before iterating through rooms
            availableCount = 0;
            occupiedCount = 0;
            cleaningCount = 0;

            while (rs.next()) {
                String roomNumber = rs.getString("number");
                String status = rs.getString("status");
                String roomType = rs.getString("roomtype");
                if (roomType == null || roomType.isEmpty()) {
                    // fallback to mod logic if not set
                    roomType = determineRoomType(roomNumber);
                }
                // Format lastCleaned in rooms tab (if not 'In Progress')
                String lastCleaned = status.equals("Cleaning") ? "In Progress" : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());

                allRooms.add(new Room(roomNumber, roomType, status, lastCleaned));

                // Count by status
                switch (status) {
                    case "Available": availableCount++; break;
                    case "Occupied": occupiedCount++; break;
                    case "Cleaning": cleaningCount++; break;
                }
            }
            // Update UI labels if they exist
            if (availableCountLabel != null) availableCountLabel.setText(availableCount + " Rooms");
            if (occupiedCountLabel != null) occupiedCountLabel.setText(occupiedCount + " Rooms");
            if (cleaningCountLabel != null) cleaningCountLabel.setText(cleaningCount + " Rooms");
        } catch (SQLException e) {
            System.out.println("Error loading rooms: " + e.getMessage());
            showAlert("Database Error", "Failed to load rooms: " + e.getMessage());
        }
    }

    private String determineRoomType(String roomNumber) {
        // This is a placeholder logic - in a real system, this would come from the database
        int num = Integer.parseInt(roomNumber);
        if (num % 3 == 0) return "Suite";
        if (num % 2 == 0) return "Deluxe";
        return "Standard";
    }    private void loadBookingsFromDatabase() {
        allBookings.clear(); // Clear existing bookings
        System.out.println("\n=== Loading bookings from database ===");
        String query = "SELECT b.id, c.name, r.number, b.check_in, b.check_out " +
                "FROM booking b " +
                "JOIN customer c ON b.customer_id = c.id " +
                "JOIN room r ON b.room_id = r.id " +
                "ORDER BY b.check_in DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String bookingId = "B" + rs.getInt("id");
                String customerName = rs.getString("name");
                String roomNumber = rs.getString("number");

                // Format check-in and check-out dates for bookings (with time if present, else just date)
                Date checkInDate = rs.getDate("check_in");
                Date checkOutDate = rs.getDate("check_out");
                String checkIn = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(checkInDate);
                String checkOut = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(checkOutDate);                // Determine booking status based on current date and check-in/check-out dates
                String status = determineBookingStatus(checkInDate, checkOutDate);
                System.out.println(String.format("Booking %s: Room %s, Check-in: %s, Check-out: %s, Status: %s",
                    bookingId, roomNumber, checkIn, checkOut, status));

                Booking booking = new Booking(bookingId, customerName, roomNumber, checkIn, checkOut, status);                allBookings.add(booking);
                System.out.println("Added booking to list: " + bookingId);
            }
            System.out.println("\nTotal bookings loaded: " + allBookings.size());
            System.out.println("=== Current table items: " + bookingTable.getItems().size() + " ===\n");
        } catch (SQLException e) {
            System.out.println("Error loading bookings: " + e.getMessage());
            showAlert("Database Error", "Failed to load bookings: " + e.getMessage());
        }
    }    private String determineBookingStatus(Date checkIn, Date checkOut) {
        try {
            // Use today's actual date for proper status determination
            Date today = new Date(System.currentTimeMillis());
            System.out.println("Processing booking status:");
            System.out.println("  Today's date: " + today);
            System.out.println("  Check-in date: " + checkIn);
            System.out.println("  Check-out date: " + checkOut);
            
            if (today.after(checkOut)) {
                System.out.println("  Status: Completed (today is after check-out)");
                return "Completed";
            } else if (today.before(checkIn)) {
                System.out.println("  Status: Reserved (today is before check-in)");
                return "Reserved";
            } else {
                System.out.println("  Status: Active (booking is ongoing)");
                return "Active";
            }
        } catch (Exception e) {
            System.out.println("Error determining booking status: " + e.getMessage());
            e.printStackTrace();
            return "Unknown";
        }
    }

    private void loadAccessLogsFromDatabase() {
        allAccessLogs.clear();
        String query = "SELECT al.access_time, r.number, al.access_type, al.details, " +
                "cust.name AS customer_name, staff.name AS staff_name, al.customer_id, al.staff_id " +
                "FROM accesslog al " +
                "JOIN room r ON al.room_id = r.id " +
                "LEFT JOIN customer cust ON al.customer_id = cust.id " +
                "LEFT JOIN customer staff ON al.staff_id = staff.id " +
                "ORDER BY al.access_time DESC";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Timestamp accessTime = rs.getTimestamp("access_time");
                // Format access log timestamp with time
                String formattedTime = accessTime.toLocalDateTime()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                String roomNumber = rs.getString("number");
                String accessType = rs.getString("access_type");
                String details = rs.getString("details");
                int customerId = rs.getInt("customer_id");
                int staffId = rs.getInt("staff_id");
                String person;
                if (staffId > 0) {
                    person = rs.getString("staff_name");
                } else if (customerId > 0) {
                    person = rs.getString("customer_name");
                } else {
                    person = "Unknown";
                }
                allAccessLogs.add(new AccessLog(formattedTime, roomNumber, accessType, person, details));
            }
        } catch (SQLException e) {
            System.out.println("Error loading access logs: " + e.getMessage());
            showAlert("Database Error", "Failed to load access logs: " + e.getMessage());
        }
    }

    private void loadRequestsFromDatabase() {
        allRequests.clear();
        // Update the SQL query to join with the customer table and retrieve the email
        String query = "SELECT sr.customer_name AS name, c.email, sr.requested_at " +
                       "FROM staff_requests sr " +
                       "JOIN customer c ON sr.customer_id = c.id " +
                       "ORDER BY sr.requested_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String email = rs.getString("email");
                String requestedAt = rs.getString("requested_at");
                allRequests.add(new Request(name, email, requestedAt));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load staff requests: " + e.getMessage());
        }
        requestsTable.refresh();
    }

    private void handleAcceptRequest(Request request) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(request.getEmail());
        try (Connection conn = DatabaseUtil.getConnection()) {
            // Find customer by email
            String updateSql = "UPDATE customer SET role = 'Staff' WHERE email = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, sanitizedEmail);
                int updated = updateStmt.executeUpdate();
                if (updated > 0) {
                    String deleteSql = "DELETE FROM staff_requests WHERE customer_id = (SELECT id FROM customer WHERE email = ?)";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setString(1, sanitizedEmail);
                        int deleted = deleteStmt.executeUpdate();
                        if (deleted > 0) {
                            showAlert("Request Accepted", "Staff account approved for: " + request.getName());
                        } else {
                            showAlert("Error", "Request not found in staff_requests table.");
                        }
                    }
                } else {
                    showAlert("Error", "No matching customer found for this request. Make sure the email exists in the customer table.");
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to approve staff request: " + e.getMessage());
        }
        loadRequestsFromDatabase();
    }

    private void handleDeclineRequest(Request request) {
        String sanitizedEmail = InputSanitizer.sanitizeEmail(request.getEmail());
        try (Connection conn = DatabaseUtil.getConnection()) {
            String deleteSql = "DELETE FROM staff_requests WHERE customer_id = (SELECT id FROM customer WHERE email = ?)";
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setString(1, sanitizedEmail);
                int deleted = deleteStmt.executeUpdate();
                if (deleted > 0) {
                    showAlert("Request Declined", "Staff account request declined for: " + request.getName());
                } else {
                    showAlert("Error", "Request not found in staff_requests table.");
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to decline staff request: " + e.getMessage());
        }
        loadRequestsFromDatabase();
    }

    private void populateRoomSelectComboBox() {
        ObservableList<String> roomOptions = FXCollections.observableArrayList();

        for (Room room : allRooms) {
            roomOptions.add(room.getRoomNumber() + " - " + room.getRoomType());
        }

        roomSelect.setItems(roomOptions);
    }

    private String getCurrentSelectedStatus() {
        // This would typically be determined by which button was last pressed
        // For simplicity, we'll just use the current status label text
        String currentText = currentStatusLabel.getText();
        return currentText.replace("Current Status: ", "");
    }

    private void prepareStatusUpdate(String status) {
        if (roomSelect.getValue() != null) {
            currentStatusLabel.setText("Current Status: " + status);

            // Set style for status label
            switch (status) {
                case "Available":
                    currentStatusLabel.setStyle("-fx-text-fill: #2e7d32;");
                    break;
                case "Occupied":
                    currentStatusLabel.setStyle("-fx-text-fill: #c62828;");
                    break;
                case "Cleaning":
                    currentStatusLabel.setStyle("-fx-text-fill: #f57c00;");
                    break;
            }
        } else {
            showAlert("Error", "Please select a room first");
        }
    }

    private void updateRoomStatus(String roomNumber, String newStatus) {
        String sanitizedRoomNumber = InputSanitizer.sanitizeInput(roomNumber);
        String sanitizedStatus = InputSanitizer.sanitizeInput(newStatus);
        
        String query = "UPDATE room SET status = ? WHERE number = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sanitizedStatus);
            stmt.setString(2, sanitizedRoomNumber);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                // Update the room in our collection
                for (Room room : allRooms) {
                    if (room.getRoomNumber().equals(roomNumber)) {
                        String oldStatus = room.getStatus(); // Store old status

                        room.setStatus(newStatus);

                        // Update lastCleaned if status is Available (assuming cleaning is done)
                        if (newStatus.equals("Available")) {
                            room.setLastCleaned(getCurrentTimestamp());
                        } else if (newStatus.equals("Cleaning")) {
                            room.setLastCleaned("In Progress");
                        }

                        // Adjust counts dynamically
                        // Safeguard to prevent negative counts
                        if (oldStatus.equals("Available") && availableCount > 0) availableCount--;
                        if (oldStatus.equals("Occupied") && occupiedCount > 0) occupiedCount--;
                        if (oldStatus.equals("Cleaning") && cleaningCount > 0) cleaningCount--;

                        // Increment new status count
                        if (newStatus.equals("Available")) availableCount++;
                        if (newStatus.equals("Occupied")) occupiedCount++;
                        if (newStatus.equals("Cleaning")) cleaningCount++;

                        // Update UI labels
                        if (availableCountLabel != null) availableCountLabel.setText(availableCount + " Rooms");
                        if (occupiedCountLabel != null) occupiedCountLabel.setText(occupiedCount + " Rooms");
                        if (cleaningCountLabel != null) cleaningCountLabel.setText(cleaningCount + " Rooms");

                        break;
                    }
                }

                // Refresh the table
                roomTable.refresh();
            } else {
                showAlert("Error", "Failed to update room status. Room not found.");
            }

        } catch (SQLException e) {
            System.out.println("Error updating room status: " + e.getMessage());
            showAlert("Database Error", "Failed to update room status: " + e.getMessage());
        }
    }

    private void addAccessLog(String roomNumber, String accessType, String person, String details) {
        String sanitizedRoomNumber = InputSanitizer.sanitizeInput(roomNumber);
        String sanitizedAccessType = InputSanitizer.sanitizeInput(accessType);
        String sanitizedPerson = InputSanitizer.sanitizeName(person);
        String sanitizedDetails = InputSanitizer.sanitizeInput(details);
        
        int roomId = getRoomIdFromNumber(sanitizedRoomNumber);

        if (roomId == -1) {
            System.out.println("Room not found: " + sanitizedRoomNumber);
            return;
        }

        // Add the log to the database, now with access_type and details
        String query = "INSERT INTO accesslog (access_time, room_id, customer_id, staff_id, access_type, details) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, roomId);

            String role = App.getLoggedInRole();
            if ("Staff".equalsIgnoreCase(role)) {
                stmt.setNull(3, java.sql.Types.INTEGER); // customer_id
                stmt.setInt(4, App.getLoggedInStaffId()); // staff_id
            } else {
                stmt.setInt(3, App.getLoggedInCustomerId()); // customer_id
                stmt.setNull(4, java.sql.Types.INTEGER); // staff_id
            }
            stmt.setString(5, sanitizedAccessType);
            stmt.setString(6, sanitizedDetails);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {                // Create a new log entry for the UI
                AccessLog log = new AccessLog(
                        getCurrentTimestamp(),
                        sanitizedRoomNumber,
                        sanitizedAccessType,
                        sanitizedPerson,
                        sanitizedDetails
                );

                // Add to our collection
                allAccessLogs.add(0, log); // Add at the top
                accessLogTable.refresh();
            }

        } catch (SQLException e) {
            System.out.println("Error adding access log: " + e.getMessage());
            showAlert("Database Error", "Failed to add access log: " + e.getMessage());
        }
    }

    private int getRoomIdFromNumber(String roomNumber) {
        String query = "SELECT id FROM room WHERE number = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, roomNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error getting room ID: " + e.getMessage());
        }

        return -1;  // Room not found
    }    private void filterBookings(String searchText) {
        String sanitizedSearch = InputSanitizer.sanitizeInput(searchText);
        ObservableList<Booking> filteredList = FXCollections.observableArrayList();
        
        for (Booking booking : allBookings) {
            boolean matchesSearch = sanitizedSearch == null || sanitizedSearch.isEmpty() ||
                booking.getCustomerName().toLowerCase().contains(sanitizedSearch.toLowerCase()) ||
                booking.getBookingId().toLowerCase().contains(sanitizedSearch.toLowerCase());
            
            if (matchesSearch) {
                filteredList.add(booking);
            }
        }
        
        bookingTable.setItems(filteredList);
    }

    private void filterAccessLogs(LocalDate date) {
        if (date == null) {
            accessLogTable.setItems(allAccessLogs);
            return;
        }

        // Filter logs based on date
        ObservableList<AccessLog> filteredList = FXCollections.observableArrayList();
        String datePrefix = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        for (AccessLog log : allAccessLogs) {
            if (log.getTimestamp().startsWith(datePrefix)) {
                filteredList.add(log);
            }
        }

        accessLogTable.setItems(filteredList);
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private <T> Callback<TableColumn<T, Void>, TableCell<T, Void>> createActionButtonCellFactory(
            java.util.function.Consumer<T> action, String buttonLabel, String buttonStyle) {
        return new Callback<>() {
            @Override
            public TableCell<T, Void> call(final TableColumn<T, Void> param) {
                return new TableCell<>() {
                    private final Button btn = new Button(buttonLabel);
                    {
                        btn.setStyle(buttonStyle);
                        btn.setOnAction(_ -> {
                            T data = getTableView().getItems().get(getIndex());
                            action.accept(data);
                        });
                    }

                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btn);
                        }
                    }
                };
            }
        };
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Model classes (unchanged)
    public static class Room {
        private final String roomNumber;
        private final String roomType;
        private String status;
        private String lastCleaned;

        public Room(String roomNumber, String roomType, String status, String lastCleaned) {
            this.roomNumber = roomNumber;
            this.roomType = roomType;
            this.status = status;
            this.lastCleaned = lastCleaned;
        }

        public String getRoomNumber() { return roomNumber; }
        public String getRoomType() { return roomType; }
        public String getStatus() { return status; }
        public String getLastCleaned() { return lastCleaned; }

        public void setStatus(String status) { this.status = status; }
        public void setLastCleaned(String lastCleaned) { this.lastCleaned = lastCleaned; }
    }

    public static class Booking {
        private final String bookingId;
        private final String customerName;
        private final String roomNumber;
        private final String checkIn;
        private final String checkOut;
        private final String status;

        public Booking(String bookingId, String customerName, String roomNumber,
                       String checkIn, String checkOut, String status) {
            this.bookingId = bookingId;
            this.customerName = customerName;
            this.roomNumber = roomNumber;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.status = status;
        }

        public String getBookingId() { return bookingId; }
        public String getCustomerName() { return customerName; }
        public String getRoomNumber() { return roomNumber; }
        public String getCheckIn() { return checkIn; }
        public String getCheckOut() { return checkOut; }
        public String getStatus() { return status; }
    }

    public static class AccessLog {
        private final String timestamp;
        private final String roomNumber;
        private final String accessType;
        private final String person;
        private final String details;

        public AccessLog(String timestamp, String roomNumber, String accessType,
                         String person, String details) {
            this.timestamp = timestamp;
            this.roomNumber = roomNumber;
            this.accessType = accessType;
            this.person = person;
            this.details = details;
        }

        public String getTimestamp() { return timestamp; }
        public String getRoomNumber() { return roomNumber; }
        public String getAccessType() { return accessType; }
        public String getPerson() { return person; }
        public String getDetails() { return details; }
    }

    public static class Request {
        private final SimpleStringProperty name;
        private final SimpleStringProperty email;
        private final SimpleStringProperty requestedAt;

        public Request(String name, String email, String requestedAt) {
            this.name = new SimpleStringProperty(name);
            this.email = new SimpleStringProperty(email);
            this.requestedAt = new SimpleStringProperty(requestedAt);
        }

        public String getName() { return name.get(); }
        public String getEmail() { return email.get(); }
        public String getRequestedAt() { return requestedAt.get(); }
    }
}