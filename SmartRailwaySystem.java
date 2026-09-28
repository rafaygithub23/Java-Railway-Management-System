import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// ===================== CUSTOM EXCEPTIONS =====================

class TrainFullException extends Exception {
    public TrainFullException(String message) { super(message); }
}

class InvalidDateException extends Exception {
    public InvalidDateException(String message) { super(message); }
}

class UserNotFoundException extends Exception {
    public UserNotFoundException(String message) { super(message); }
}

class DuplicateTrainException extends Exception {
    public DuplicateTrainException(String message) { super(message); }
}

// ===================== INTERFACES =====================

interface ReportGenerator {
    String generateDailyReport();
}

interface Authentication {
    boolean validateCredentials(String email, String password);
}

// ===================== ABSTRACT CLASS: User =====================

abstract class User implements Authentication {
    protected int id;
    protected String name;
    protected String email;
    protected String password;

    public User() {
        this.id = 0; this.name = "Unknown"; this.email = "unknown@mail.com"; this.password = "default";
    }

    public User(int id, String name, String email, String password) {
        this.id = id; this.name = name; this.email = email; this.password = password;
    }

    public abstract void login();

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }

    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public boolean validateCredentials(String email, String password) {
        return this.email.equals(email) && this.password.equals(password);
    }

    @Override
    public String toString() {
        return "ID: " + id + " | Name: " + name + " | Email: " + email;
    }
}

// ===================== PASSENGER CLASS =====================

class Passenger extends User implements ReportGenerator {
    private String[] travelHistory;
    private int historyCount;

    public Passenger(String name) {
        super(); this.name = name; this.travelHistory = new String[50]; this.historyCount = 0;
    }

    public Passenger(String name, String email) {
        super(0, name, email, "pass123"); this.travelHistory = new String[50]; this.historyCount = 0;
    }

    public Passenger(int id, String name, String email, String password) {
        super(id, name, email, password); this.travelHistory = new String[50]; this.historyCount = 0;
    }

    @Override
    public void login() { System.out.println("Passenger " + name + " logged in successfully."); }

    public Ticket bookTicket(Train train, String route, String date, int seatNumber)
            throws TrainFullException, InvalidDateException {
        try {
            java.time.LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (DateTimeParseException e) {
            throw new InvalidDateException("Invalid date: " + date + ". Please use dd-MM-yyyy format.");
        }
        if (train.getAvailableSeats() <= 0) {
            throw new TrainFullException("Train " + train.getTrainName() + " is FULL! No seats available.");
        }
        if (!train.isSeatAvailable(seatNumber)) {
            throw new TrainFullException("Seat " + seatNumber + " is already booked on " + train.getTrainName() + "!");
        }
        train.bookSeat(seatNumber);
        Ticket ticket = new Ticket(this, train, route, date, seatNumber);
        ticket.saveToFile();
        if (historyCount < travelHistory.length) { travelHistory[historyCount++] = ticket.toString(); }
        return ticket;
    }

    public void viewBookings() {
        System.out.println("--- Bookings for " + name + " ---");
        if (historyCount == 0) { System.out.println("No bookings yet."); return; }
        for (int i = 0; i < historyCount; i++) { System.out.println((i + 1) + ". " + travelHistory[i]); }
    }

    public String[] getTravelHistory() { return travelHistory; }
    public int getHistoryCount() { return historyCount; }

    @Override
    public String generateDailyReport() { return "Passenger Report - " + name + ": Total Bookings = " + historyCount; }
}

// ===================== RAILWAY STAFF CLASS =====================

class RailwayStaff extends User {
    private String trainAssigned;
    private String[][] schedule;

    public RailwayStaff(int id, String name, String email, String password, String trainAssigned) {
        super(id, name, email, password);
        this.trainAssigned = trainAssigned;
        this.schedule = new String[7][5];
        initDefaultSchedule();
    }

    private void initDefaultSchedule() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            schedule[i][0] = days[i]; schedule[i][1] = "Karachi-Lahore";
            schedule[i][2] = "Lahore-Islamabad"; schedule[i][3] = "08:00"; schedule[i][4] = "18:00";
        }
    }

    @Override
    public void login() { System.out.println("Railway Staff " + name + " logged in successfully."); }

    public void updateAvailability(Train train, int newSeats) {
        train.setAvailableSeats(newSeats);
        System.out.println("Updated " + train.getTrainName() + " availability to " + newSeats + " seats.");
    }

    public void updateSchedule(int dayIndex, int colIndex, String value) {
        if (dayIndex >= 0 && dayIndex < 7 && colIndex >= 0 && colIndex < 5) { schedule[dayIndex][colIndex] = value; }
    }

    public String[][] getSchedule() { return schedule; }
    public String getTrainAssigned() { return trainAssigned; }
    public void setTrainAssigned(String t) { this.trainAssigned = t; }

    public String getScheduleString() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Weekly Schedule: ").append(trainAssigned).append(" ===\n");
        sb.append(String.format("%-12s %-20s %-20s %-10s %-10s\n", "Day", "Route 1", "Route 2", "Depart", "Arrive"));
        for (int i = 0; i < 7; i++) {
            sb.append(String.format("%-12s %-20s %-20s %-10s %-10s\n",
                    schedule[i][0], schedule[i][1], schedule[i][2], schedule[i][3], schedule[i][4]));
        }
        return sb.toString();
    }
}

// ===================== ADMIN CLASS =====================

class Admin extends User implements ReportGenerator {

    public Admin(int id, String name, String email, String password) { super(id, name, email, password); }

    @Override
    public void login() { System.out.println("Admin " + name + " logged in successfully."); }

    public void addTrain(List<Train> trains, Train newTrain) throws DuplicateTrainException {
        for (Train t : trains) {
            if (t.getTrainNumber() == newTrain.getTrainNumber()) {
                throw new DuplicateTrainException("Train #" + newTrain.getTrainNumber() + " already exists! Cannot add duplicate.");
            }
        }
        trains.add(newTrain);
    }

    public String viewAllRecords() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== All Booking Records ===\n");
        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader("tickets.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) { sb.append(line).append("\n"); found = true; }
        } catch (IOException e) { sb.append("No records found.\n"); }
        if (!found) sb.append("No bookings yet.\n");
        return sb.toString();
    }

    @Override
    public String generateDailyReport() {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader("tickets.txt"))) {
            while (reader.readLine() != null) count++;
        } catch (IOException e) { /* file not found yet */ }
        return "Daily Report: Total Tickets Booked = " + count;
    }
}

// ===================== TRAIN CLASS =====================

class Train {
    private int trainNumber;
    private String trainName;
    private String category;
    private String route;
    private int totalSeats;
    private int availableSeats;
    private boolean[] seatMap;
    private double fare;

    public Train(int trainNumber, String trainName, String category, String route, int totalSeats, double fare) {
        this.trainNumber = trainNumber; this.trainName = trainName; this.category = category;
        this.route = route; this.totalSeats = totalSeats; this.availableSeats = totalSeats;
        this.seatMap = new boolean[totalSeats + 1]; this.fare = fare;
    }

    public boolean bookSeat(int seatNum) {
        if (seatNum < 1 || seatNum > totalSeats) return false;
        if (seatMap[seatNum]) return false;
        seatMap[seatNum] = true; availableSeats--; return true;
    }

    public boolean isSeatAvailable(int seatNum) {
        if (seatNum < 1 || seatNum > totalSeats) return false;
        return !seatMap[seatNum];
    }

    public int getTrainNumber() { return trainNumber; }
    public String getTrainName() { return trainName; }
    public String getCategory() { return category; }
    public String getRoute() { return route; }
    public int getTotalSeats() { return totalSeats; }
    public int getAvailableSeats() { return availableSeats; }
    public double getFare() { return fare; }
    public void setAvailableSeats(int s) { this.availableSeats = s; }

    @Override
    public String toString() {
        return trainNumber + " | " + trainName + " [" + category + "] | " + route + " | Seats: " + availableSeats + "/" + totalSeats + " | Fare: Rs." + fare;
    }
}

// ===================== TICKET CLASS =====================

class Ticket {
    private String ticketId;
    private String passengerName;
    private String passengerEmail;
    private int trainNumber;
    private String trainName;
    private String route;
    private String date;
    private int seatNumber;
    private double fare;

    public Ticket(Passenger passenger, Train train, String route, String date, int seatNumber) {
        this.ticketId = "TKT" + (System.currentTimeMillis() % 100000);
        this.passengerName = passenger.getName(); this.passengerEmail = passenger.getEmail();
        this.trainNumber = train.getTrainNumber(); this.trainName = train.getTrainName();
        this.route = route; this.date = date; this.seatNumber = seatNumber; this.fare = train.getFare();
    }

    public void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("tickets.txt", true))) {
            writer.println(ticketId + "," + passengerName + "," + passengerEmail + "," + trainNumber + "," + trainName + "," + route + "," + date + "," + seatNumber + "," + fare);
        } catch (IOException e) { System.out.println("Error saving ticket: " + e.getMessage()); }
    }

    public static List<String> getAllTickets() {
        List<String> tickets = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("tickets.txt"))) {
            String line; while ((line = reader.readLine()) != null) { tickets.add(line); }
        } catch (IOException e) { /* file not found */ }
        return tickets;
    }

    public String getTicketId() { return ticketId; }
    public String getPassengerName() { return passengerName; }
    public String getRoute() { return route; }
    public String getDate() { return date; }
    public int getSeatNumber() { return seatNumber; }
    public String getTrainName() { return trainName; }
    public double getFare() { return fare; }

    @Override
    public String toString() { return ticketId + " | " + trainName + " | " + route + " | " + date + " | Seat:" + seatNumber + " | Rs." + fare; }
}

// ===================== FILE DATABASE =====================

class FileDatabase {
    private static final String USERS_FILE = "users.txt";

    // secret ID PASS for Staff And Admin
    private static final String STAFF_EMAIL = "staff@railway.pk";
    private static final String STAFF_PASSWORD = "staff123";

    private static final String ADMIN_EMAIL = "admin@railway.pk";
    private static final String ADMIN_PASSWORD = "admin123";

    // Save ONLY Passengers to file
    public static void saveUser(User user, String role) {
        if (!role.equals("Passenger")) return; // Security: Only passengers saved
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE, true))) {
            writer.println(role + "," + user.getId() + "," + user.getName() + "," + user.getEmail() + "," + user.getPassword());
        } catch (IOException e) { System.out.println("Error saving user: " + e.getMessage()); }
    }

    // Find Passenger from file
    public static User findPassenger(String email, String password) throws UserNotFoundException {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 5 && parts[0].equals("Passenger") && parts[3].equals(email) && parts[4].equals(password)) {
                    return new Passenger(Integer.parseInt(parts[1]), parts[2], email, password);
                }
            }
        } catch (IOException e) { /* file not found */ }
        throw new UserNotFoundException("Invalid email or password for Passenger!");
    }

    // Validate Hardcoded Staff/Admin Login
    public static User validateSpecialUser(String email, String password, String role) throws UserNotFoundException {
        if (role.equals("Staff")) {
            if (email.equals(STAFF_EMAIL) && password.equals(STAFF_PASSWORD)) {
                return new RailwayStaff(9001, "Official Staff", email, password, "General");
            } else {
                throw new UserNotFoundException("Invalid Staff Credentials! Unauthorized access denied.");
            }
        }
        else if (role.equals("Admin")) {
            if (email.equals(ADMIN_EMAIL) && password.equals(ADMIN_PASSWORD)) {
                return new Admin(9999, "Super Admin", email, password);
            } else {
                throw new UserNotFoundException("Invalid Admin Credentials! Unauthorized access denied.");
            }
        }
        throw new UserNotFoundException("Invalid Role Selected!");
    }

    public static boolean emailExists(String email) {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4 && parts[3].equals(email)) return true;
            }
        } catch (IOException e) { /* file not found */ }
        return false;
    }

    public static int getNextId() {
        int maxId = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    try { maxId = Math.max(maxId, Integer.parseInt(parts[1])); }
                    catch (NumberFormatException e) { /* skip */ }
                }
            }
        } catch (IOException e) { /* file not found */ }
        return maxId + 1;
    }

    public static List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line; while ((line = reader.readLine()) != null) users.add(line);
        } catch (IOException e) { /* file not found */ }
        return users;
    }
}

// ===================== TRAIN SEARCH =====================

class TrainSearch {
    public static List<Train> searchTrain(List<Train> trains, String route) {
        List<Train> results = new ArrayList<>();
        for (Train t : trains) { if (t.getRoute().toLowerCase().contains(route.toLowerCase())) results.add(t); }
        return results;
    }

    public static List<Train> searchTrain(List<Train> trains, String route, String category) {
        List<Train> results = new ArrayList<>();
        for (Train t : trains) {
            if (t.getRoute().toLowerCase().contains(route.toLowerCase()) && t.getCategory().equalsIgnoreCase(category)) results.add(t);
        }
        return results;
    }
}

// ===================== MAIN JAVAFX APPLICATION =====================

public class SmartRailwaySystem extends Application {

    private Stage primaryStage;
    private User currentUser;
    private List<Train> trainList = new ArrayList<>();

    private final String DARK_BG = "#1a1a2e";
    private final String CARD_BG = "#16213e";
    private final String ACCENT = "#0f3460";
    private final String HIGHLIGHT = "#e94560";
    private final String SUCCESS = "#27ae60";

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        primaryStage.setTitle("🚂 Smart Railway Booking System - Pakistan Railways");
        loadDefaultTrains();
        showLoginScene();
        primaryStage.show();
    }

    private void loadDefaultTrains() {
        trainList.add(new Train(101, "Karakoram Express", "Express", "Karachi-Lahore", 20, 4500));
        trainList.add(new Train(102, "Tezgam Express", "Express", "Karachi-Islamabad", 20, 5500));
        trainList.add(new Train(103, "Chenab Express", "Local", "Lahore-Faisalabad", 15, 800));
        trainList.add(new Train(104, "Shalimar Express", "Express", "Lahore-Karachi", 10, 4000));
        trainList.add(new Train(105, "Khyber Mail", "Express", "Peshawar-Karachi", 10, 5000));
        trainList.add(new Train(106, "Bolan Mail", "Local", "Quetta-Karachi", 15, 1200));
        trainList.add(new Train(107, "Cargo Express", "Cargo", "Karachi-Lahore", 5, 8000));
        trainList.add(new Train(108, "Sukkur Express", "Local", "Karachi-Sukkur", 15, 1000));
    }

    private Button makeButton(String text, String color) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: " + HIGHLIGHT + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 6; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 25; -fx-background-radius: 6; -fx-cursor: hand;"));
        return btn;
    }

    private TextField makeTextField(String prompt) {
        TextField tf = new TextField(); tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-prompt-text-fill: #888; -fx-padding: 8; -fx-background-radius: 4; -fx-font-size: 13px;");
        return tf;
    }

    private Label makeLabel(String text, int size) {
        Label lbl = new Label(text); lbl.setTextFill(Color.WHITE); lbl.setFont(Font.font("Arial", FontWeight.BOLD, size));
        return lbl;
    }

    private void showAlert(String title, String msg, Alert.AlertType type) {
        Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(msg); alert.showAndWait();
    }

    //                     LOGIN / SIGNUP SCENE

    private void showLoginScene() {
        VBox root = new VBox(20); root.setAlignment(Pos.CENTER); root.setPadding(new Insets(30)); root.setStyle("-fx-background-color: " + DARK_BG + ";");

        Label title = makeLabel(" Pakistan Railways", 28);
        Label subtitle = makeLabel("Smart Booking & Management System", 16);
        subtitle.setTextFill(Color.LIGHTGRAY);

        Label roleLabel = makeLabel("Select Role:", 14);
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Passenger", "Staff", "Admin");
        roleCombo.setValue("Passenger");
        roleCombo.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-font-size: 13px; -fx-padding: 8;");

        TextField emailField = makeTextField("Enter Email");
        PasswordField passField = new PasswordField(); passField.setPromptText("Enter Password");
        passField.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-prompt-text-fill: #888; -fx-padding: 8; -fx-background-radius: 4; -fx-font-size: 13px;");

        Button loginBtn = makeButton("LOGIN", ACCENT);
        Button signupBtn = makeButton("SIGN UP", SUCCESS);

        Label resultLabel = new Label(); resultLabel.setTextFill(Color.LIGHTYELLOW); resultLabel.setWrapText(true);

        // Login button action
        loginBtn.setOnAction(e -> {
            String email = emailField.getText().trim();
            String pass = passField.getText().trim();
            String role = roleCombo.getValue();

            if (email.isEmpty() || pass.isEmpty()) { resultLabel.setText("⚠ Please fill in all fields!"); return; }

            try {
                // SEPARATE LOGIN LOGIC BASED ON ROLE
                if (role.equals("Passenger")) {
                    currentUser = FileDatabase.findPassenger(email, pass); // Checks users.txt file
                } else {
                    currentUser = FileDatabase.validateSpecialUser(email, pass, role); // Checks hardcoded secrets
                }

                currentUser.login();

                if (role.equals("Passenger")) showPassengerScene();
                else if (role.equals("Staff")) showStaffScene();
                else if (role.equals("Admin")) showAdminScene();

            } catch (UserNotFoundException ex) {
                resultLabel.setText("❌ " + ex.getMessage());
            }
        });

        // Signup button action - ✅ BLOCKS STAFF/ADMIN SIGNUP
        signupBtn.setOnAction(e -> {
            String role = roleCombo.getValue();

            // Security Check: Only passengers can sign up
            if (role.equals("Staff") || role.equals("Admin")) {
                resultLabel.setText("⛔ Only Passengers can sign up! Staff/Admin use assigned IDs.");
                return;
            }

            String email = emailField.getText().trim();
            String pass = passField.getText().trim();

            if (email.isEmpty() || pass.isEmpty()) { resultLabel.setText("⚠ Please fill in all fields!"); return; }

            if (FileDatabase.emailExists(email)) { resultLabel.setText("⚠ Email already registered! Please login."); return; }

            int newId = FileDatabase.getNextId();
            Passenger p = new Passenger(newId, "User" + newId, email, pass);
            FileDatabase.saveUser(p, "Passenger");
            resultLabel.setText("✅ Signup successful! Now login.");
        });

        VBox formBox = new VBox(12); formBox.setAlignment(Pos.CENTER); formBox.setPadding(new Insets(25));
        formBox.setStyle("-fx-background-color: " + CARD_BG + "; -fx-background-radius: 12;"); formBox.setMaxWidth(420);
        formBox.getChildren().addAll(title, subtitle, new Separator(), roleLabel, roleCombo, emailField, passField, new HBox(15, loginBtn, signupBtn), resultLabel);

        root.getChildren().add(formBox);
        primaryStage.setScene(new Scene(root, 700, 550));
    }

    // ============================================================
    //                    PASSENGER SCENE
    // ============================================================
    private void showPassengerScene() {
        Passenger passenger = (Passenger) currentUser;
        BorderPane root = new BorderPane(); root.setStyle("-fx-background-color: " + DARK_BG + ";");

        HBox topBar = new HBox(15); topBar.setPadding(new Insets(15)); topBar.setStyle("-fx-background-color: " + CARD_BG + ";"); topBar.setAlignment(Pos.CENTER_LEFT);
        Label welcomeLabel = makeLabel("🚂 Welcome, " + passenger.getName(), 18);
        Button logoutBtn = makeButton("Logout", HIGHLIGHT); logoutBtn.setOnAction(e -> { currentUser = null; showLoginScene(); });
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(welcomeLabel, spacer, logoutBtn); root.setTop(topBar);

        TabPane tabPane = new TabPane(); tabPane.setStyle("-fx-background-color: " + DARK_BG + ";");

        // Tab 1: Search & Book
        Tab bookTab = new Tab("🔍 Search & Book"); bookTab.setClosable(false);
        VBox bookBox = new VBox(15); bookBox.setPadding(new Insets(20)); bookBox.setStyle("-fx-background-color: " + DARK_BG + ";");

        ComboBox<String> catCombo = new ComboBox<>(); catCombo.getItems().addAll("All", "Express", "Local", "Cargo"); catCombo.setValue("All");
        catCombo.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-padding: 6;");
        TextField routeField = makeTextField("Search Route (e.g. Karachi-Lahore)");
        Button searchBtn = makeButton("Search Trains", ACCENT);
        TextArea resultsArea = new TextArea(); resultsArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #eee; -fx-font-size: 12px;"); resultsArea.setPrefRowCount(8); resultsArea.setEditable(false);

        TextField trainNumField = makeTextField("Train Number (e.g. 101)");
        TextField dateField = makeTextField("Date (dd-MM-yyyy)");
        TextField seatField = makeTextField("Seat Number");
        Button bookBtn = makeButton("🎫 Book Ticket", SUCCESS);
        Label bookResult = new Label(); bookResult.setWrapText(true); bookResult.setTextFill(Color.LIGHTYELLOW);

        searchBtn.setOnAction(e -> {
            String route = routeField.getText().trim(); String category = catCombo.getValue();
            List<Train> found = category.equals("All") ? TrainSearch.searchTrain(trainList, route) : TrainSearch.searchTrain(trainList, route, category);
            resultsArea.clear();
            if (found.isEmpty()) { resultsArea.setText("No trains found."); }
            else { for (Train t : found) resultsArea.appendText(t.toString() + "\n"); }
        });

        bookBtn.setOnAction(e -> {
            try {
                int trainNum = Integer.parseInt(trainNumField.getText().trim()); String date = dateField.getText().trim(); int seatNum = Integer.parseInt(seatField.getText().trim());
                Train selected = null; for (Train t : trainList) { if (t.getTrainNumber() == trainNum) { selected = t; break; } }
                if (selected == null) { bookResult.setText("❌ Train not found!"); return; }
                Ticket ticket = passenger.bookTicket(selected, selected.getRoute(), date, seatNum);
                bookResult.setText("✅ Ticket Booked!\n" + ticket.toString());
            } catch (NumberFormatException ex) { bookResult.setText("❌ Enter valid numbers.");
            } catch (TrainFullException | InvalidDateException ex) { bookResult.setText("❌ " + ex.getMessage()); showAlert("Error!", ex.getMessage(), Alert.AlertType.ERROR); }
        });

        bookBox.getChildren().addAll(catCombo, routeField, searchBtn, resultsArea, new Separator(), trainNumField, dateField, seatField, bookBtn, bookResult);
        bookTab.setContent(new ScrollPane(bookBox));

        // Tab 2: My Bookings
        Tab bookingsTab = new Tab("📋 My Bookings"); bookingsTab.setClosable(false);
        VBox bookingsBox = new VBox(15); bookingsBox.setPadding(new Insets(20)); bookingsBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        TextArea bookingsArea = new TextArea(); bookingsArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #eee; -fx-font-size: 12px;"); bookingsArea.setEditable(false);
        Button refreshBtn = makeButton("Refresh", ACCENT);
        refreshBtn.setOnAction(e -> {
            bookingsArea.clear(); List<String> tickets = Ticket.getAllTickets(); boolean f = false;
            for (String t : tickets) { if (t.split(",").length >= 2 && t.split(",")[1].equals(passenger.getName())) { bookingsArea.appendText(t + "\n"); f = true; } }
            if (!f) bookingsArea.setText("No bookings found.");
        });
        bookingsBox.getChildren().addAll(refreshBtn, bookingsArea); bookingsTab.setContent(bookingsBox);

        // Tab 3: Report
        Tab reportTab = new Tab("📊 Report"); reportTab.setClosable(false);
        VBox reportBox = new VBox(15); reportBox.setPadding(new Insets(20)); reportBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        reportBox.getChildren().addAll(makeLabel("Your Report:", 16), new Label(passenger.generateDailyReport()));
        reportTab.setContent(reportBox);

        tabPane.getTabs().addAll(bookTab, bookingsTab, reportTab); root.setCenter(tabPane);
        primaryStage.setScene(new Scene(root, 800, 650));
    }

    // ============================================================
    //                      STAFF SCENE
    // ============================================================
    private void showStaffScene() {
        RailwayStaff staff = (RailwayStaff) currentUser;
        BorderPane root = new BorderPane(); root.setStyle("-fx-background-color: " + DARK_BG + ";");

        HBox topBar = new HBox(15); topBar.setPadding(new Insets(15)); topBar.setStyle("-fx-background-color: " + CARD_BG + ";"); topBar.setAlignment(Pos.CENTER_LEFT);
        Label welcomeLabel = makeLabel("👷 Staff: " + staff.getName(), 18);
        Button logoutBtn = makeButton("Logout", HIGHLIGHT); logoutBtn.setOnAction(e -> { currentUser = null; showLoginScene(); });
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(welcomeLabel, spacer, logoutBtn); root.setTop(topBar);

        TabPane tabPane = new TabPane(); tabPane.setStyle("-fx-background-color: " + DARK_BG + ";");

        // Tab 1: Schedule
        Tab scheduleTab = new Tab("📅 Schedule"); scheduleTab.setClosable(false);
        VBox schedBox = new VBox(15); schedBox.setPadding(new Insets(20)); schedBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        TextArea schedArea = new TextArea(staff.getScheduleString()); schedArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #eee;"); schedArea.setEditable(false);
        schedBox.getChildren().addAll(makeLabel("Weekly Schedule:", 16), schedArea); scheduleTab.setContent(schedBox);

        // Tab 2: Update Seats
        Tab updateTab = new Tab("🔄 Update Seats"); updateTab.setClosable(false);
        VBox updateBox = new VBox(15); updateBox.setPadding(new Insets(20)); updateBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        TextArea trainsArea = new TextArea(); trainsArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #eee;"); trainsArea.setEditable(false);
        StringBuilder sb = new StringBuilder(); for (Train t : trainList) sb.append(t.toString()).append("\n"); trainsArea.setText(sb.toString());

        TextField trainNumField = makeTextField("Train Number"); TextField seatsField = makeTextField("New Available Seats");
        Button updateBtn = makeButton("Update", SUCCESS); Label updateResult = new Label(); updateResult.setWrapText(true); updateResult.setTextFill(Color.LIGHTYELLOW);
        updateBtn.setOnAction(e -> {
            try {
                int trainNum = Integer.parseInt(trainNumField.getText().trim()); int newSeats = Integer.parseInt(seatsField.getText().trim());
                Train found = null; for (Train t : trainList) { if (t.getTrainNumber() == trainNum) { found = t; break; } }
                if (found == null) { updateResult.setText("❌ Train not found!"); return; }
                staff.updateAvailability(found, newSeats); updateResult.setText("✅ Updated!");
                StringBuilder sb2 = new StringBuilder(); for (Train t : trainList) sb2.append(t.toString()).append("\n"); trainsArea.setText(sb2.toString());
            } catch (NumberFormatException ex) { updateResult.setText("❌ Enter valid numbers!"); }
        });
        updateBox.getChildren().addAll(makeLabel("All Trains:", 16), trainsArea, trainNumField, seatsField, updateBtn, updateResult); updateTab.setContent(updateBox);

        tabPane.getTabs().addAll(scheduleTab, updateTab); root.setCenter(tabPane);
        primaryStage.setScene(new Scene(root, 800, 650));
    }

    // ============================================================
    //                      ADMIN SCENE
    // ============================================================
    private void showAdminScene() {
        Admin admin = (Admin) currentUser;
        BorderPane root = new BorderPane(); root.setStyle("-fx-background-color: " + DARK_BG + ";");

        HBox topBar = new HBox(15); topBar.setPadding(new Insets(15)); topBar.setStyle("-fx-background-color: " + CARD_BG + ";"); topBar.setAlignment(Pos.CENTER_LEFT);
        Label welcomeLabel = makeLabel("🔑 Admin: " + admin.getName(), 18);
        Button logoutBtn = makeButton("Logout", HIGHLIGHT); logoutBtn.setOnAction(e -> { currentUser = null; showLoginScene(); });
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        topBar.getChildren().addAll(welcomeLabel, spacer, logoutBtn); root.setTop(topBar);

        TabPane tabPane = new TabPane(); tabPane.setStyle("-fx-background-color: " + DARK_BG + ";");

        // Tab 1: Add Train
        Tab addTrainTab = new Tab("🚂 Add Train"); addTrainTab.setClosable(false);
        VBox addBox = new VBox(12); addBox.setPadding(new Insets(20)); addBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        TextField numField = makeTextField("Train Number"); TextField nameField = makeTextField("Train Name");
        ComboBox<String> catField = new ComboBox<>(); catField.getItems().addAll("Express", "Local", "Cargo"); catField.setValue("Express"); catField.setStyle("-fx-background-color: #0f3460; -fx-text-fill: white; -fx-padding: 6;");
        TextField routeField = makeTextField("Route"); TextField seatsField = makeTextField("Total Seats"); TextField fareField = makeTextField("Fare");
        Button addBtn = makeButton("➕ Add Train", SUCCESS); Label addResult = new Label(); addResult.setWrapText(true); addResult.setTextFill(Color.LIGHTYELLOW);
        addBtn.setOnAction(e -> {
            try {
                int num = Integer.parseInt(numField.getText().trim()); String name = nameField.getText().trim(); String cat = catField.getValue();
                String route = routeField.getText().trim(); int seats = Integer.parseInt(seatsField.getText().trim()); double fare = Double.parseDouble(fareField.getText().trim());
                if (name.isEmpty() || route.isEmpty()) { addResult.setText("⚠ Fill all fields!"); return; }
                admin.addTrain(trainList, new Train(num, name, cat, route, seats, fare));
                addResult.setText("✅ Train '" + name + "' added!");
            } catch (NumberFormatException ex) { addResult.setText("❌ Enter valid numbers.");
            } catch (DuplicateTrainException ex) { addResult.setText("❌ " + ex.getMessage()); showAlert("Duplicate!", ex.getMessage(), Alert.AlertType.ERROR); }
        });
        addBox.getChildren().addAll(numField, nameField, catField, routeField, seatsField, fareField, addBtn, addResult); addTrainTab.setContent(addBox);

        // Tab 2: Bookings
        Tab bookingsTab = new Tab("📋 Bookings"); bookingsTab.setClosable(false);
        VBox allBookBox = new VBox(15); allBookBox.setPadding(new Insets(20)); allBookBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        TextArea allBookingsArea = new TextArea(); allBookingsArea.setStyle("-fx-control-inner-background: #16213e; -fx-text-fill: #eee;"); allBookingsArea.setEditable(false);
        Button refreshBookings = makeButton("Refresh", ACCENT); refreshBookings.setOnAction(e -> allBookingsArea.setText(admin.viewAllRecords()));
        allBookBox.getChildren().addAll(refreshBookings, allBookingsArea); bookingsTab.setContent(allBookBox);

        // Tab 3: Chart
        Tab chartTab = new Tab("📊 Chart"); chartTab.setClosable(false);
        VBox chartBox = new VBox(15); chartBox.setPadding(new Insets(20)); chartBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        CategoryAxis xAxis = new CategoryAxis(); NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis); barChart.setStyle("-fx-background-color: " + CARD_BG + ";");
        Button refreshChart = makeButton("Refresh Chart", ACCENT);
        refreshChart.setOnAction(e -> {
            barChart.getData().clear(); int ex=0, lo=0, ca=0;
            for (String t : Ticket.getAllTickets()) { String[] p = t.split(","); if(p.length>=5){ for(Train tr: trainList){ if(tr.getTrainName().equals(p[4])){ if(tr.getCategory().equals("Express"))ex++; else if(tr.getCategory().equals("Local"))lo++; else ca++; break; }}}}
            XYChart.Series<String, Number> series = new XYChart.Series<>(); series.setName("Bookings");
            series.getData().add(new XYChart.Data<>("Express", ex)); series.getData().add(new XYChart.Data<>("Local", lo)); series.getData().add(new XYChart.Data<>("Cargo", ca));
            barChart.getData().add(series);
        });
        chartBox.getChildren().addAll(refreshChart, barChart); chartTab.setContent(chartBox);

        // Tab 4: Report
        Tab reportTab = new Tab("📄 Report"); reportTab.setClosable(false);
        VBox reportBox = new VBox(15); reportBox.setPadding(new Insets(20)); reportBox.setStyle("-fx-background-color: " + DARK_BG + ";");
        Label repLabel = new Label(admin.generateDailyReport()); repLabel.setTextFill(Color.LIGHTGREEN); repLabel.setFont(Font.font("Arial", 14));
        Button refreshReport = makeButton("Refresh", ACCENT); refreshReport.setOnAction(e -> repLabel.setText(admin.generateDailyReport()));
        reportBox.getChildren().addAll(makeLabel("Daily Report:", 16), refreshReport, repLabel); reportTab.setContent(reportBox);

        tabPane.getTabs().addAll(addTrainTab, bookingsTab, chartTab, reportTab); root.setCenter(tabPane);
        primaryStage.setScene(new Scene(root, 900, 700));
    }

    // ============================================================
    //                       MAIN METHOD
    // ============================================================
    public static void main(String[] args) {
        System.out.println("====================================================");
        System.out.println("   Smart Railway Booking & Management System");
        System.out.println("====================================================\n");

        // Console Demonstrations
        Passenger p1 = new Passenger("Ahmed");
        Passenger p2 = new Passenger("Sara", "sara@mail.com");
        p1.login(); p2.login();

        RailwayStaff staff = new RailwayStaff(10, "Kamran", "kamran@rail.pk", "staff123", "Karakoram Express");
        staff.login();

        Admin admin = new Admin(99, "Root", "admin@rail.pk", "admin123");
        admin.login();

        try {
            Train fullTrain = new Train(200, "Test", "Express", "X", 1, 1000);
            fullTrain.bookSeat(1);
            p1.bookTicket(fullTrain, "X", "25-12-2025", 1);
        } catch (TrainFullException e) { System.out.println("✅ TrainFullException caught: " + e.getMessage()); }
        catch (InvalidDateException e) { System.out.println("✅ InvalidDateException caught: " + e.getMessage()); }

        System.out.println("\n--- Launching GUI ---");
        launch(args);
    }
}

