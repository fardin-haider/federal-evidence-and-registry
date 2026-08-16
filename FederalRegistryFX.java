import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class FederalRegistryFX extends Application {
    private DataManager db;
    private User loggedInUser = null;

    private Stage primaryStage;
    private Scene loginScene;
    private Scene dashboardScene;

    // UI Components for Dashboard
    private Label welcomeLabel;
    private TabPane tabPane;
    private TextArea caseDisplay, suspectDisplay, evidenceDisplay, searchDisplay;
    private Tab adminTab;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        db = new DataManager();
        db.loadData();
        seedAdminUser();

        primaryStage.setTitle("Federal Evidence & Suspect Registry");

        primaryStage.setOnCloseRequest(e -> {
            db.saveData();
            System.exit(0);
        });

        // Initialize Scenes
        loginScene = new Scene(createLoginPane(), 900, 650);
        dashboardScene = new Scene(createDashboardPane(), 900, 650);

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    private GridPane createLoginPane() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background-color: #282c34;");

        Label scenetitle = new Label("FEDERAL REGISTRY LOGIN");
        scenetitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        scenetitle.setStyle("-fx-text-fill: white;");
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("Username:");
        userName.setStyle("-fx-text-fill: white;");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Password:");
        pw.setStyle("-fx-text-fill: white;");
        grid.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        grid.add(pwBox, 1, 2);

        Button btn = new Button("Authenticate");
        btn.setStyle("-fx-background-color: #61afef; -fx-text-fill: black; -fx-font-weight: bold;");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        btn.setOnAction(e -> {
            String username = userTextField.getText();
            String password = pwBox.getText();

            for (User u : db.users.values()) {
                if (u.getUsername().equals(username) && u.getPassword().equals(password)) {
                    loggedInUser = u;
                    welcomeLabel.setText("Agent " + u.getLastName() + " | Role: " + u.getRole());

                    adminTab.setDisable(!u.getRole().equals("Admin"));

                    refreshAllDisplays();
                    primaryStage.setScene(dashboardScene);
                    userTextField.clear();
                    pwBox.clear();
                    return;
                }
            }
            showAlert(Alert.AlertType.ERROR, "Login Error", "Access Denied. Invalid credentials.");
        });

        return grid;
    }

    private BorderPane createDashboardPane() {
        BorderPane borderPane = new BorderPane();

        BorderPane header = new BorderPane();
        header.setStyle("-fx-background-color: #21252b; -fx-padding: 10px;");
        welcomeLabel = new Label();
        welcomeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setOnAction(e -> {
            loggedInUser = null;
            primaryStage.setScene(loginScene);
        });

        header.setLeft(welcomeLabel);
        header.setRight(logoutBtn);
        borderPane.setTop(header);

        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab caseTab = new Tab("Case Management", createCasePane());
        Tab suspectTab = new Tab("Suspect Registry", createSuspectPane());
        Tab evidenceTab = new Tab("Evidence Locker", createEvidencePane());
        Tab searchTab = new Tab("Search & Reports", createSearchPane());
        adminTab = new Tab("Admin Console", createAdminPane());

        tabPane.getTabs().addAll(caseTab, suspectTab, evidenceTab, searchTab, adminTab);
        borderPane.setCenter(tabPane);

        return borderPane;
    }

    private BorderPane createCasePane() {
        BorderPane pane = new BorderPane();
        caseDisplay = new TextArea();
        caseDisplay.setEditable(false);
        caseDisplay.setFont(Font.font("Monospaced", 14));
        pane.setCenter(caseDisplay);

        HBox btnBox = new HBox(10);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);

        Button addBtn = new Button("Create New Case");
        Button statusBtn = new Button("Update Status");

        addBtn.setOnAction(e -> {
            String title = promptText("New Case", "Enter Case Title:");
            if (title != null && !title.trim().isEmpty()) {
                String id = "C-" + LocalDate.now().getYear() + "-" + String.format("%03d", db.cases.size() + 1);
                db.cases.put(id, new Case(id, title, LocalDate.now().toString(), "Open"));
                refreshAllDisplays();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Case " + id + " created.");
            }
        });

        statusBtn.setOnAction(e -> {
            String id = promptText("Update Status", "Enter Case ID:");
            if (id != null && db.cases.containsKey(id)) {
                String status = promptChoice("Update Status", "Select Status:", Arrays.asList("Open", "Closed", "Cold"), db.cases.get(id).getStatus());
                if (status != null) {
                    db.cases.get(id).setStatus(status);
                    refreshAllDisplays();
                }
            } else if (id != null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Case ID not found.");
            }
        });

        btnBox.getChildren().addAll(addBtn, statusBtn);
        pane.setBottom(btnBox);
        return pane;
    }

    private BorderPane createSuspectPane() {
        BorderPane pane = new BorderPane();
        suspectDisplay = new TextArea();
        suspectDisplay.setEditable(false);
        suspectDisplay.setFont(Font.font("Monospaced", 14));
        pane.setCenter(suspectDisplay);

        HBox btnBox = new HBox(10);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);

        Button addBtn = new Button("Register Suspect");
        Button linkBtn = new Button("Link to Case");
        Button statusBtn = new Button("Update Status");

        addBtn.setOnAction(e -> {
            String fName = promptText("Register", "First Name:");
            if (fName == null) return;
            String lName = promptText("Register", "Last Name:");
            String dob = promptText("Register", "DOB (YYYY-MM-DD):");
            String traits = promptText("Register", "Physical Traits:");

            String id = "S-" + String.format("%04d", db.suspects.size() + 1);
            db.suspects.put(id, new Suspect(id, fName, lName, dob, traits, "Wanted"));
            refreshAllDisplays();
        });

        linkBtn.setOnAction(e -> {
            String sId = promptText("Link Suspect", "Enter Suspect ID:");
            String cId = promptText("Link Suspect", "Enter Case ID to link to:");
            if (sId != null && cId != null && db.suspects.containsKey(sId) && db.cases.containsKey(cId)) {
                db.suspects.get(sId).linkCase(cId);
                db.cases.get(cId).addSuspect(sId);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Suspect linked to Case successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Invalid Suspect ID or Case ID.");
            }
        });

        statusBtn.setOnAction(e -> {
            String id = promptText("Update Status", "Suspect ID:");
            if (id != null && db.suspects.containsKey(id)) {
                String status = promptChoice("Update Status", "Select Status:", Arrays.asList("Wanted", "In Custody", "Under Surveillance", "Cleared"), db.suspects.get(id).getStatus());
                if (status != null) {
                    db.suspects.get(id).setStatus(status);
                    refreshAllDisplays();
                }
            }
        });

        btnBox.getChildren().addAll(addBtn, linkBtn, statusBtn);
        pane.setBottom(btnBox);
        return pane;
    }

    private BorderPane createEvidencePane() {
        BorderPane pane = new BorderPane();
        evidenceDisplay = new TextArea();
        evidenceDisplay.setEditable(false);
        evidenceDisplay.setFont(Font.font("Monospaced", 14));
        pane.setCenter(evidenceDisplay);

        HBox btnBox = new HBox(10);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);

        Button addBtn = new Button("Log Evidence");
        Button custodyBtn = new Button("Update Custody/Status");

        addBtn.setOnAction(e -> {
            String cId = promptText("Log Evidence", "Target Case ID:");
            if (cId != null && db.cases.containsKey(cId)) {
                String type = promptChoice("Evidence Type", "Select Type:", Arrays.asList("Weapon", "Digital", "Document", "Biological"), "Weapon");
                String desc = promptText("Description", "Enter brief description:");

                String id = "EV-" + String.format("%04d", db.evidence.size() + 1);
                Evidence ev = new Evidence(id, cId, type, desc, "In Storage");
                String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                ev.logCustody("Logged by Agent " + loggedInUser.getLastName() + " at " + time);

                db.evidence.put(id, ev);
                db.cases.get(cId).addEvidence(id);
                refreshAllDisplays();
            } else if (cId != null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Case not found.");
            }
        });

        custodyBtn.setOnAction(e -> {
            String id = promptText("Update Custody", "Evidence ID:");
            if (id != null && db.evidence.containsKey(id)) {
                Evidence ev = db.evidence.get(id);
                String status = promptChoice("Update Status", "New Status:", Arrays.asList("In Storage", "At Lab/Forensics", "Released/Destroyed"), ev.getStatus());
                if (status != null) {
                    ev.setStatus(status);
                    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    ev.logCustody("Status -> " + status + " by Agent " + loggedInUser.getLastName() + " at " + time);
                    refreshAllDisplays();
                }
            }
        });

        btnBox.getChildren().addAll(addBtn, custodyBtn);
        pane.setBottom(btnBox);
        return pane;
    }

    private BorderPane createSearchPane() {
        BorderPane pane = new BorderPane();
        searchDisplay = new TextArea();
        searchDisplay.setEditable(false);
        searchDisplay.setFont(Font.font("Monospaced", 14));
        pane.setCenter(searchDisplay);

        HBox topBox = new HBox(10);
        topBox.setPadding(new Insets(10));
        topBox.setAlignment(Pos.CENTER);

        TextField searchField = new TextField();
        searchField.setPromptText("Enter keyword or ID...");
        Button searchBtn = new Button("Search");
        Button exportBtn = new Button("Export Case Dossier");

        searchBtn.setOnAction(e -> {
            String q = searchField.getText().toLowerCase();
            StringBuilder res = new StringBuilder("--- SEARCH RESULTS ---\n\n");

            db.cases.values().stream().filter(c -> c.getCaseId().toLowerCase().contains(q) || c.getTitle().toLowerCase().contains(q))
                    .forEach(c -> res.append("CASE: ").append(c.getCaseId()).append(" - ").append(c.getTitle()).append("\n"));

            db.suspects.values().stream().filter(s -> s.getId().toLowerCase().contains(q) || s.getLastName().toLowerCase().contains(q) || s.getFirstName().toLowerCase().contains(q))
                    .forEach(s -> res.append("SUSPECT: ").append(s.getId()).append(" - ").append(s.getFirstName()).append(" ").append(s.getLastName()).append("\n"));

            db.evidence.values().stream().filter(ev -> ev.getEvidenceId().toLowerCase().contains(q))
                    .forEach(ev -> res.append("EVIDENCE: ").append(ev.getEvidenceId()).append(" (Linked to ").append(ev.getCaseId()).append(")\n"));

            if (res.length() == 25) res.append("No results found.");
            searchDisplay.setText(res.toString());
        });

        exportBtn.setOnAction(e -> {
            String id = promptText("Export Dossier", "Enter Case ID:");
            if (id != null && db.cases.containsKey(id)) {
                generateComprehensiveDossier(id);
                showAlert(Alert.AlertType.INFORMATION, "Success", "Dossier exported: Dossier_" + id + ".txt");
            } else if (id != null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Case ID not found.");
            }
        });

        topBox.getChildren().addAll(new Label("Keyword:"), searchField, searchBtn, exportBtn);
        pane.setTop(topBox);
        return pane;
    }

    private VBox createAdminPane() {
        VBox pane = new VBox(20);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(50));

        Label warningLabel = new Label("ADMINISTRATOR CONSOLE\nWarning: Deletions are permanent.");
        warningLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        warningLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button addUserBtn = new Button("Register New Agent");
        Button deleteBtn = new Button("Permanently Delete Record");

        addUserBtn.setOnAction(e -> {
            if (!loggedInUser.getRole().equals("Admin")) return;

            String user = promptText("New Agent", "Enter Username:");
            if (user == null) return;
            String pass = promptText("New Agent", "Enter Password:");
            String last = promptText("New Agent", "Enter Agent Last Name:");

            String id = "U-" + String.format("%03d", db.users.size() + 1);
            db.users.put(id, new User(id, "Agent", last, "N/A", user, pass, "Field Agent"));
            showAlert(Alert.AlertType.INFORMATION, "Success", "Agent " + last + " added to registry.");
        });

        deleteBtn.setOnAction(e -> {
            if (!loggedInUser.getRole().equals("Admin")) return;

            String id = promptText("Delete Record", "Enter Exact ID (Case, Suspect, or Evidence):");
            if (id != null) {
                if (db.cases.remove(id) != null || db.suspects.remove(id) != null || db.evidence.remove(id) != null) {
                    refreshAllDisplays();
                    showAlert(Alert.AlertType.INFORMATION, "Deleted", "Record " + id + " permanently deleted.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Not Found", "Record ID does not exist.");
                }
            }
        });

        pane.getChildren().addAll(warningLabel, addUserBtn, deleteBtn);
        return pane;
    }

    private void refreshAllDisplays() {
        StringBuilder cb = new StringBuilder("--- ACTIVE CASES ---\n\n");
        for (Case c : db.cases.values()) cb.append(String.format("[%s] %s | Status: %s\n", c.getCaseId(), c.getTitle(), c.getStatus()));
        if (caseDisplay != null) caseDisplay.setText(cb.toString());

        StringBuilder sb = new StringBuilder("--- SUSPECT REGISTRY ---\n\n");
        for (Suspect s : db.suspects.values()) sb.append(String.format("[%s] %s %s | Status: %s\n", s.getId(), s.getFirstName(), s.getLastName(), s.getStatus()));
        if (suspectDisplay != null) suspectDisplay.setText(sb.toString());

        StringBuilder eb = new StringBuilder("--- EVIDENCE LOCKER ---\n\n");
        for (Evidence ev : db.evidence.values()) eb.append(String.format("[%s] Case: %s | Status: %s\n", ev.getEvidenceId(), ev.getCaseId(), ev.getStatus()));
        if (evidenceDisplay != null) evidenceDisplay.setText(eb.toString());
    }

    private void generateComprehensiveDossier(String caseId) {
        Case c = db.cases.get(caseId);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("Dossier_" + caseId + ".txt"))) {
            bw.write("========================================\n");
            bw.write("       CASE DOSSIER: " + c.getCaseId() + "\n");
            bw.write("========================================\n");
            bw.write("Title: " + c.getTitle() + "\nStatus: " + c.getStatus() + "\nOpened: " + c.getDateOpened() + "\n\n");

            bw.write("--- LINKED SUSPECTS ---\n");
            for (Suspect s : db.suspects.values()) {
                if (s.getStatus() != null) {
                    bw.write("[" + s.getId() + "] " + s.getFirstName() + " " + s.getLastName() + " - " + s.getStatus() + "\n");
                }
            }

            bw.write("\n--- LOGGED EVIDENCE ---\n");
            for (Evidence ev : db.evidence.values()) {
                if (ev.getCaseId().equals(caseId)) {
                    bw.write("[" + ev.getEvidenceId() + "] Status: " + ev.getStatus() + "\n");
                }
            }
            bw.write("\nEnd of Official Report.\n");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void seedAdminUser() {
        if (db.users.isEmpty()) {
            db.users.put("U-001", new User("U-001", "System", "Admin", "1980-01-01", "admin", "admin123", "Admin"));
        }
    }

    private String promptText(String title, String header) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String promptChoice(String title, String header, List<String> choices, String defaultChoice) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}