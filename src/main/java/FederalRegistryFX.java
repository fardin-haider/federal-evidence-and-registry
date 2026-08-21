import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

/**
 * FederalRegistryFX.java - Modern Federal Evidence & Suspect Registry UI.
 *
 * ============================================================================
 * KEY UI/UX ARCHITECTURE & DESIGN HIGHLIGHTS
 * ============================================================================
 * 1. REAL-TIME MULTI-CRITERIA SEARCH & FILTER BARS
 *    Every management tab (Cases, Suspects, Evidence, and Admin Agents) contains
 *    a top filter toolbar. It combines:
 *      - An instant live text search box (matching IDs, titles, descriptions, names).
 *      - Appendable / category-specific dropdown menus (Status, Type, Role).
 *      - A quick clear ('✕') button to reset filters.
 *    Implementation: Powered by JavaFX `FilteredList` and `SortedList` wrappers
 *    around master ObservableLists, allowing continuous sorting and instant UI updates.
 *
 * 2. COMPREHENSIVE EDIT DIALOGS (EASIER EDITING)
 *    Instead of only allowing status changes, users now have dedicated "Edit Record"
 *    workflows with pre-populated dialogs:
 *      - Edit Case: Update Title and Status.
 *      - Edit Suspect: Update First/Last Name, DOB (via DatePicker), Traits, Status, and Aliases.
 *      - Edit Evidence: Reassign Case, change Category/Type, update Description, and Status.
 *      - Edit Agent: Modify Username, Name, Role, or Password.
 *
 * 3. RIGHT-CLICK CONTEXT MENUS & ACCESSIBILITY
 *    Tables support intuitive right-click context menus for quick actions ("Edit Record",
 *    "Update Status", "View Details") in addition to standard toolbar buttons and double-clicks.
 *
 * 4. CENTRALIZED UNIFIED THEME
 *    Applied via external CSS (style.css) across all windows, DialogPanes, and Alerts.
 * ============================================================================
 */
public class FederalRegistryFX extends Application {
    private DataManager db;
    private User loggedInUser = null;

    private Stage primaryStage;
    private Scene loginScene;
    private Scene dashboardScene;
    private String stylesheetUrl;

    // CSS Style Class Constants
    private static final String CLASS_HEADER_BAR = "header-bar";
    private static final String CLASS_CONTENT_PANE = "content-pane";
    private static final String CLASS_WELCOME_LABEL = "welcome-label";
    private static final String CLASS_LOGIN_TITLE = "login-title";
    private static final String CLASS_DANGER_TEXT = "danger-text";
    private static final String CLASS_DANGER_BUTTON = "danger-button";
    private static final String CLASS_FILTER_BAR = "filter-bar";
    private static final String CLASS_ACTION_BAR = "action-bar";

    // Master Observable Collections (Holds ground-truth data for JavaFX)
    private ObservableList<Case> masterCaseList = FXCollections.observableArrayList();
    private ObservableList<Suspect> masterSuspectList = FXCollections.observableArrayList();
    private ObservableList<Evidence> masterEvidenceList = FXCollections.observableArrayList();
    private ObservableList<User> masterUserList = FXCollections.observableArrayList();

    // Filtered Collections (Driven by search fields & dropdown menus)
    private FilteredList<Case> filteredCaseList;
    private FilteredList<Suspect> filteredSuspectList;
    private FilteredList<Evidence> filteredEvidenceList;
    private FilteredList<User> filteredUserList;

    // UI Tables
    private TableView<Case> caseTable;
    private TableView<Suspect> suspectTable;
    private TableView<Evidence> evidenceTable;
    private TableView<User> userTable;

    // Header & Tab Navigation
    private Label welcomeLabel;
    private TabPane tabPane;
    private Tab adminTab;

    // Search Tab Components
    private TextArea searchDisplay;
    private TextField globalSearchField;
    private ComboBox<String> searchEntityFilter;
    private ComboBox<String> searchStatusFilter;

    // Admin Console Components
    private ComboBox<String> deleteTypeChoice;
    private ListView<String> deleteListView;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        db = new DataManager();
        db.loadData();
        seedAdminUser();
        seedSampleData();
        stylesheetUrl = getStylesheetUrl();

        primaryStage.setTitle("Federal Evidence & Suspect Registry");

        primaryStage.setOnCloseRequest(e -> {
            db.saveData();
            System.exit(0);
        });

        // Initialize Scenes
        loginScene = new Scene(createLoginPane(), 920, 680);
        dashboardScene = new Scene(createDashboardPane(), 960, 700);

        if (stylesheetUrl != null) {
            loginScene.getStylesheets().add(stylesheetUrl);
            dashboardScene.getStylesheets().add(stylesheetUrl);
        }

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    /**
     * Seeds realistic dummy data if the database is currently empty.
     * If you want to reset your data at any point to this sample set,
     * simply delete the 'registry_data.dat' file from your project root.
     */
    private void seedSampleData() {
        // 1. SEED AGENTS / USERS
        if (db.users.isEmpty()) {
            db.users.put("U-001", new User("U-001", "System", "Admin", "1980-01-01", "admin", "admin123", "Admin"));
            db.users.put("U-002", new User("U-002", "Sarah", "Jenkins", "1989-05-14", "sjenkins", "agent123", "Field Agent"));
            db.users.put("U-003", new User("U-003", "David", "Miller", "1984-11-20", "dmiller", "agent123", "Field Agent"));
        }

        // 2. SEED CASES, SUSPECTS, AND EVIDENCE (Only if no cases exist)
        if (db.cases.isEmpty()) {
            // ==========================================
            // CASE 1: Operation Nightfall (Cybercrime)
            // ==========================================
            Case c1 = new Case("C-2026-001", "Operation Nightfall", "2026-01-15", "Open");

            Suspect s1 = new Suspect("S-0001", "Marcus", "Vance", "1988-04-12", "6'1\", scar on left forearm, athletic build", "Wanted");
            s1.addAlias("Zero");
            s1.addAlias("GhostProtocol");
            s1.linkCase("C-2026-001");
            c1.addSuspect("S-0001");

            Suspect s2 = new Suspect("S-0002", "Elena", "Rostova", "1993-09-27", "5'7\", green eyes, raven tattoo on right shoulder", "Under Surveillance");
            s2.addAlias("Cipher");
            s2.addAlias("Nika");
            s2.linkCase("C-2026-001");
            c1.addSuspect("S-0002");

            Evidence ev1 = new Evidence("EV-0001", "C-2026-001", "Digital", "Encrypted 2TB NVMe SSD containing offshore server logs and intrusion scripts", "At Lab/Forensics");
            ev1.logCustody("Seized during raid at safehouse by Agent Jenkins at 2026-01-16 03:45");
            ev1.logCustody("Transferred to Cyber Forensics Division by Agent Jenkins at 2026-01-16 09:15");
            c1.addEvidence("EV-0001");

            Evidence ev2 = new Evidence("EV-0002", "C-2026-001", "Document", "Forged diplomatic passport under the alias 'Marc Vance'", "In Storage");
            ev2.logCustody("Logged into Central Evidence Locker by Agent Miller at 2026-01-17 11:20");
            c1.addEvidence("EV-0002");

            db.cases.put(c1.getCaseId(), c1);
            db.suspects.put(s1.getId(), s1);
            db.suspects.put(s2.getId(), s2);
            db.evidence.put(ev1.getEvidenceId(), ev1);
            db.evidence.put(ev2.getEvidenceId(), ev2);

            // ==========================================
            // CASE 2: The Crimson Heist (Vault Robbery)
            // ==========================================
            Case c2 = new Case("C-2026-002", "The Crimson Heist", "2026-03-22", "Cold");

            Suspect s3 = new Suspect("S-0003", "Dmitri", "Volkov", "1979-12-03", "5'11\", heavy build, graying beard, slight limp", "In Custody");
            s3.addAlias("The Ghost");
            s3.addAlias("Uncle D");
            s3.linkCase("C-2026-002");
            c2.addSuspect("S-0003");

            Evidence ev3 = new Evidence("EV-0003", "C-2026-002", "Weapon", "Modified industrial thermal lance used to breach vault door", "In Storage");
            ev3.logCustody("Recovered from abandoned getaway vehicle by Agent Miller at 2026-03-23 06:10");
            c2.addEvidence("EV-0003");

            Evidence ev4 = new Evidence("EV-0004", "C-2026-002", "Biological", "Blood swab collected from shattered vault security glass", "At Lab/Forensics");
            ev4.logCustody("Sample collected by Crime Scene Unit at 2026-03-22 08:30");
            ev4.logCustody("Sent to State DNA Database for matching at 2026-03-24 14:00");
            c2.addEvidence("EV-0004");

            db.cases.put(c2.getCaseId(), c2);
            db.suspects.put(s3.getId(), s3);
            db.evidence.put(ev3.getEvidenceId(), ev3);
            db.evidence.put(ev4.getEvidenceId(), ev4);

            // ==========================================
            // CASE 3: Project Mirage (Counterfeiting Ring)
            // ==========================================
            Case c3 = new Case("C-2026-003", "Project Mirage", "2025-11-04", "Closed");

            Suspect s4 = new Suspect("S-0004", "Arthur", "Pendelton", "1965-06-18", "5'9\", silver hair, wire-rimmed glasses", "Cleared");
            s4.addAlias("The Architect");
            s4.linkCase("C-2026-003");
            c3.addSuspect("S-0004");

            Evidence ev5 = new Evidence("EV-0005", "C-2026-003", "Document", "Precision intaglio printing plates for counterfeit $100 bills", "In Storage");
            ev5.logCustody("Seized from basement printing press by Agent Jenkins at 2025-11-10 16:30");
            c3.addEvidence("EV-0005");

            Evidence ev6 = new Evidence("EV-0006", "C-2026-003", "Document", "Ledger detailing shell corporations and distribution contacts", "Released/Destroyed");
            ev6.logCustody("Audited and archived post-trial by Agent Admin at 2026-02-01 10:00");
            ev6.logCustody("Authorized destruction of certified duplicate copies at 2026-02-15 15:30");
            c3.addEvidence("EV-0006");

            db.cases.put(c3.getCaseId(), c3);
            db.suspects.put(s4.getId(), s4);
            db.evidence.put(ev5.getEvidenceId(), ev5);
            db.evidence.put(ev6.getEvidenceId(), ev6);

            // ==========================================
            // CASE 4: Shadow Harbor Smuggling (Trafficking)
            // ==========================================
            Case c4 = new Case("C-2026-004", "Shadow Harbor Smuggling", "2026-07-10", "Open");

            // Marcus Vance is cross-linked to this case as well
            s1.linkCase("C-2026-004");
            c4.addSuspect("S-0001");

            Suspect s5 = new Suspect("S-0005", "Tariq", "Al-Mansoor", "1985-02-14", "6'3\", broad shoulders, burn mark on right hand", "Wanted");
            s5.addAlias("Anchor");
            s5.addAlias("Captain T");
            s5.linkCase("C-2026-004");
            c4.addSuspect("S-0005");

            Evidence ev7 = new Evidence("EV-0007", "C-2026-004", "Weapon", "Crate of 12 military-grade automatic pistols with filed serial numbers", "In Storage");
            ev7.logCustody("Interception at Shipping Container 409-B by Agent Miller at 2026-07-11 01:20");
            c4.addEvidence("EV-0007");

            Evidence ev8 = new Evidence("EV-0008", "C-2026-004", "Digital", "Encrypted satellite phone with GPS coordinates and harbor call logs", "At Lab/Forensics");
            ev8.logCustody("Recovered from harbor warehouse dock by Agent Jenkins at 2026-07-11 02:00");
            c4.addEvidence("EV-0008");

            db.cases.put(c4.getCaseId(), c4);
            db.suspects.put(s5.getId(), s5);
            db.evidence.put(ev7.getEvidenceId(), ev7);
            db.evidence.put(ev8.getEvidenceId(), ev8);
        }
    }

    /** =========================================================================
     * LOGIN INTERFACE
     * ========================================================================= */
    private GridPane createLoginPane() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(30));

        Label scenetitle = new Label("FEDERAL REGISTRY AUTHENTICATION");
        scenetitle.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        scenetitle.getStyleClass().add(CLASS_LOGIN_TITLE);
        grid.add(scenetitle, 0, 0, 2, 1);

        Label userName = new Label("Username:");
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        userTextField.setPromptText("Enter agent username");
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Password:");
        grid.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter secure password");
        grid.add(pwBox, 1, 2);

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add(CLASS_DANGER_TEXT);
        grid.add(errorLabel, 0, 3, 2, 1);

        Button btn = new Button("Authenticate");
        btn.setDefaultButton(true); // Enter key submits the login
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        btn.setOnAction(e -> {
            String username = userTextField.getText().trim();
            String password = pwBox.getText();

            for (User u : db.users.values()) {
                if (u.getUsername().equalsIgnoreCase(username) && u.getPassword().equals(password)) {
                    loggedInUser = u;
                    welcomeLabel.setText("Active Agent: " + u.getLastName() + " | Role: " + u.getRole());
                    adminTab.setDisable(!u.getRole().equalsIgnoreCase("Admin"));

                    refreshAllDisplays();
                    primaryStage.setScene(dashboardScene);
                    userTextField.clear();
                    pwBox.clear();
                    errorLabel.setText("");
                    return;
                }
            }
            errorLabel.setText("Access Denied: Invalid agent credentials.");
            showAlert(Alert.AlertType.ERROR, "Authentication Error", "Access Denied. Invalid username or password.");
        });

        return grid;
    }

    /** =========================================================================
     * DASHBOARD & NAVIGATION
     * ========================================================================= */
    private BorderPane createDashboardPane() {
        BorderPane borderPane = new BorderPane();

        // Top Navigation Header
        BorderPane header = new BorderPane();
        header.getStyleClass().add(CLASS_HEADER_BAR);
        welcomeLabel = new Label();
        welcomeLabel.getStyleClass().add(CLASS_WELCOME_LABEL);

        Button logoutBtn = new Button("Logout / Lock");
        logoutBtn.setOnAction(e -> {
            loggedInUser = null;
            primaryStage.setScene(loginScene);
        });

        header.setLeft(welcomeLabel);
        header.setRight(logoutBtn);
        borderPane.setTop(header);

        // Tabbed Workspaces
        tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab caseTab = new Tab("Case Management", createCasePane());
        Tab suspectTab = new Tab("Suspect Registry", createSuspectPane());
        Tab evidenceTab = new Tab("Evidence Locker", createEvidencePane());
        Tab searchTab = new Tab("Search & Intelligence", createSearchPane());
        adminTab = new Tab("Admin Console", createAdminPane());

        tabPane.getTabs().addAll(caseTab, suspectTab, evidenceTab, searchTab, adminTab);
        borderPane.setCenter(tabPane);

        return borderPane;
    }

    /** =========================================================================
     * CASE MANAGEMENT TAB
     * ========================================================================= */
    private BorderPane createCasePane() {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add(CLASS_CONTENT_PANE);

        // --- SEARCH + DROPDOWN FILTER BAR ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Case ID, Title, or Date...");
        searchField.setPrefWidth(280);

        ComboBox<String> statusDropdown = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "Open", "Closed", "Cold"));
        statusDropdown.setValue("All Statuses");

        Button clearFilterBtn = new Button("✕ Clear");
        clearFilterBtn.setOnAction(e -> {
            searchField.clear();
            statusDropdown.setValue("All Statuses");
        });

        HBox filterBar = new HBox(12, new Label("Search Cases:"), searchField, new Label("Status:"), statusDropdown, clearFilterBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.getStyleClass().add(CLASS_FILTER_BAR);
        pane.setTop(filterBar);

        // --- TABLE VIEW SETUP ---
        caseTable = new TableView<>();
        caseTable.setPlaceholder(new Label("No cases match the current search filters."));

        TableColumn<Case, String> idCol = new TableColumn<>("Case ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCaseId()));
        idCol.setPrefWidth(120);

        TableColumn<Case, String> titleCol = new TableColumn<>("Case Title");
        titleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        titleCol.setPrefWidth(340);

        TableColumn<Case, String> openedCol = new TableColumn<>("Date Opened");
        openedCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateOpened()));
        openedCol.setPrefWidth(130);

        TableColumn<Case, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(120);

        caseTable.getColumns().addAll(idCol, titleCol, openedCol, statusCol);

        // Configure FilteredList & Sorting
        filteredCaseList = new FilteredList<>(masterCaseList, p -> true);
        Runnable updateFilter = () -> {
            String query = searchField.getText().toLowerCase().trim();
            String status = statusDropdown.getValue();
            filteredCaseList.setPredicate(c -> {
                boolean matchesStatus = status == null || status.equals("All Statuses") || c.getStatus().equalsIgnoreCase(status);
                boolean matchesQuery = query.isEmpty() || c.getCaseId().toLowerCase().contains(query)
                        || c.getTitle().toLowerCase().contains(query)
                        || c.getDateOpened().toLowerCase().contains(query);
                return matchesStatus && matchesQuery;
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> updateFilter.run());
        statusDropdown.valueProperty().addListener((obs, o, n) -> updateFilter.run());

        SortedList<Case> sortedCases = new SortedList<>(filteredCaseList);
        sortedCases.comparatorProperty().bind(caseTable.comparatorProperty());
        caseTable.setItems(sortedCases);

        // --- CONTEXT MENU & DOUBLE CLICK INTERACTION ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewItem = new MenuItem("View Details");
        MenuItem editItem = new MenuItem("Edit Case Record");
        MenuItem statusItem = new MenuItem("Quick Status Update");

        viewItem.setOnAction(e -> {
            Case selected = caseTable.getSelectionModel().getSelectedItem();
            if (selected != null) showCaseDetails(selected);
        });
        editItem.setOnAction(e -> {
            Case selected = caseTable.getSelectionModel().getSelectedItem();
            if (selected != null) openEditCaseDialog(selected);
        });
        statusItem.setOnAction(e -> {
            Case selected = caseTable.getSelectionModel().getSelectedItem();
            if (selected != null) quickUpdateCaseStatus(selected);
        });
        contextMenu.getItems().addAll(viewItem, editItem, statusItem);

        caseTable.setRowFactory(tv -> {
            TableRow<Case> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showCaseDetails(row.getItem());
                }
            });
            row.setContextMenu(contextMenu);
            return row;
        });

        pane.setCenter(caseTable);

        // --- BOTTOM ACTION BUTTON BAR ---
        HBox btnBox = new HBox(12);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getStyleClass().add(CLASS_ACTION_BAR);

        Button addBtn = new Button("Create New Case");
        Button editBtn = new Button("Edit Selected Case");
        Button statusBtn = new Button("Quick Status");
        Button viewBtn = new Button("View Full Details");

        addBtn.setOnAction(e -> openCreateCaseDialog());

        editBtn.setOnAction(e -> {
            Case selected = caseTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a case from the table to edit.");
                return;
            }
            openEditCaseDialog(selected);
        });

        statusBtn.setOnAction(e -> {
            Case selected = caseTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a case from the table first.");
                return;
            }
            quickUpdateCaseStatus(selected);
        });

        viewBtn.setOnAction(e -> {
            Case selected = caseTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a case from the table first.");
                return;
            }
            showCaseDetails(selected);
        });

        btnBox.getChildren().addAll(addBtn, editBtn, statusBtn, viewBtn);
        pane.setBottom(btnBox);
        return pane;
    }

    private void openCreateCaseDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Create Investigation Case");
        dialog.setHeaderText("Enter initial case details");
        ButtonType createButtonType = new ButtonType("Create Case", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        TextField titleField = new TextField();
        titleField.setPromptText("e.g., Operation Nightfall");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Open", "Closed", "Cold"));
        statusBox.setValue("Open");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Case Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Initial Status:"), 0, 1);
        grid.add(statusBox, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Node createBtn = dialog.getDialogPane().lookupButton(createButtonType);
        createBtn.setDisable(true);
        titleField.textProperty().addListener((obs, o, n) -> createBtn.setDisable(n.trim().isEmpty()));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == createButtonType) {
            String id = "C-" + LocalDate.now().getYear() + "-" + String.format("%03d", db.cases.size() + 1);
            Case newCase = new Case(id, titleField.getText().trim(), LocalDate.now().toString(), statusBox.getValue());
            db.cases.put(id, newCase);
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Case Created", "Case [" + id + "] has been registered.");
        }
    }

    /**
     * Complete Edit Dialog for Cases (allows editing title and status with live validation)
     */
    private void openEditCaseDialog(Case c) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Case Details");
        dialog.setHeaderText("Editing Case: " + c.getCaseId());
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        TextField titleField = new TextField(c.getTitle());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Open", "Closed", "Cold"));
        statusBox.setValue(c.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Case ID:"), 0, 0);
        grid.add(new Label(c.getCaseId() + " (Date Opened: " + c.getDateOpened() + ")"), 1, 0);
        grid.add(new Label("Case Title:"), 0, 1);
        grid.add(titleField, 1, 1);
        grid.add(new Label("Case Status:"), 0, 2);
        grid.add(statusBox, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveButtonType);
        titleField.textProperty().addListener((obs, o, n) -> saveBtn.setDisable(n.trim().isEmpty()));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            c.setTitle(titleField.getText().trim());
            c.setStatus(statusBox.getValue());
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Case Updated", "Changes to Case " + c.getCaseId() + " saved successfully.");
        }
    }

    private void quickUpdateCaseStatus(Case c) {
        String newStatus = promptChoice("Update Status", "Select new status for " + c.getCaseId() + ":",
                Arrays.asList("Open", "Closed", "Cold"), c.getStatus());
        if (newStatus != null) {
            c.setStatus(newStatus);
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Status Updated", c.getCaseId() + " status is now " + newStatus + ".");
        }
    }

    private void showCaseDetails(Case c) {
        StringBuilder sb = new StringBuilder();
        sb.append("Case ID: ").append(c.getCaseId()).append("\n");
        sb.append("Title: ").append(c.getTitle()).append("\n");
        sb.append("Status: ").append(c.getStatus()).append("\n");
        sb.append("Opened: ").append(c.getDateOpened()).append("\n\n");

        sb.append("--- LINKED SUSPECTS ---\n");
        if (c.getSuspectIds().isEmpty()) {
            sb.append("  (No suspects currently linked)\n");
        } else {
            for (String sid : c.getSuspectIds()) {
                Suspect s = db.suspects.get(sid);
                if (s != null) {
                    sb.append("  • [").append(sid).append("] ").append(s.getFirstName()).append(" ")
                            .append(s.getLastName()).append(" (Status: ").append(s.getStatus()).append(")\n");
                }
            }
        }

        sb.append("\n--- LOGGED EVIDENCE ---\n");
        boolean anyEvidence = false;
        for (Evidence ev : db.evidence.values()) {
            if (ev.getCaseId().equals(c.getCaseId())) {
                anyEvidence = true;
                sb.append("  • [").append(ev.getEvidenceId()).append("] ").append(ev.getType())
                        .append(": ").append(ev.getDescription()).append(" [").append(ev.getStatus()).append("]\n");
            }
        }
        if (!anyEvidence) sb.append("  (No evidence logged for this case)\n");

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Case File Intelligence");
        alert.setHeaderText(c.getCaseId() + ": " + c.getTitle());
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(480, 340);
        alert.getDialogPane().setContent(area);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    /** =========================================================================
     * SUSPECT REGISTRY TAB
     * ========================================================================= */
    private BorderPane createSuspectPane() {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add(CLASS_CONTENT_PANE);

        // --- SEARCH + DROPDOWN FILTER BAR ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search by Name, Suspect ID, or Traits...");
        searchField.setPrefWidth(280);

        ComboBox<String> statusDropdown = new ComboBox<>(FXCollections.observableArrayList(
                "All Statuses", "Wanted", "In Custody", "Under Surveillance", "Cleared"));
        statusDropdown.setValue("All Statuses");

        Button clearFilterBtn = new Button("✕ Clear");
        clearFilterBtn.setOnAction(e -> {
            searchField.clear();
            statusDropdown.setValue("All Statuses");
        });

        HBox filterBar = new HBox(12, new Label("Search Suspects:"), searchField, new Label("Status:"), statusDropdown, clearFilterBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.getStyleClass().add(CLASS_FILTER_BAR);
        pane.setTop(filterBar);

        // --- TABLE VIEW SETUP ---
        suspectTable = new TableView<>();
        suspectTable.setPlaceholder(new Label("No suspects match the current filters."));

        TableColumn<Suspect, String> idCol = new TableColumn<>("Suspect ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        idCol.setPrefWidth(110);

        TableColumn<Suspect, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFirstName() + " " + data.getValue().getLastName()));
        nameCol.setPrefWidth(200);

        TableColumn<Suspect, String> dobCol = new TableColumn<>("DOB");
        dobCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDateOfBirth()));
        dobCol.setPrefWidth(110);

        TableColumn<Suspect, String> traitsCol = new TableColumn<>("Physical Traits");
        traitsCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPhysicalTraits()));
        traitsCol.setPrefWidth(240);

        TableColumn<Suspect, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(140);

        suspectTable.getColumns().addAll(idCol, nameCol, dobCol, traitsCol, statusCol);

        // Configure FilteredList & Sorting
        filteredSuspectList = new FilteredList<>(masterSuspectList, p -> true);
        Runnable updateFilter = () -> {
            String query = searchField.getText().toLowerCase().trim();
            String status = statusDropdown.getValue();
            filteredSuspectList.setPredicate(s -> {
                boolean matchesStatus = status == null || status.equals("All Statuses") || s.getStatus().equalsIgnoreCase(status);
                boolean matchesQuery = query.isEmpty() || s.getId().toLowerCase().contains(query)
                        || s.getFirstName().toLowerCase().contains(query)
                        || s.getLastName().toLowerCase().contains(query)
                        || s.getPhysicalTraits().toLowerCase().contains(query);
                return matchesStatus && matchesQuery;
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> updateFilter.run());
        statusDropdown.valueProperty().addListener((obs, o, n) -> updateFilter.run());

        SortedList<Suspect> sortedSuspects = new SortedList<>(filteredSuspectList);
        sortedSuspects.comparatorProperty().bind(suspectTable.comparatorProperty());
        suspectTable.setItems(sortedSuspects);

        // --- CONTEXT MENU & DOUBLE CLICK ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem viewItem = new MenuItem("View Dossier");
        MenuItem editItem = new MenuItem("Edit Suspect Profile");
        MenuItem linkItem = new MenuItem("Link to Case");
        MenuItem statusItem = new MenuItem("Quick Status Update");

        viewItem.setOnAction(e -> {
            Suspect selected = suspectTable.getSelectionModel().getSelectedItem();
            if (selected != null) showSuspectDetails(selected);
        });
        editItem.setOnAction(e -> {
            Suspect selected = suspectTable.getSelectionModel().getSelectedItem();
            if (selected != null) openEditSuspectDialog(selected);
        });
        linkItem.setOnAction(e -> openLinkSuspectDialog());
        statusItem.setOnAction(e -> {
            Suspect selected = suspectTable.getSelectionModel().getSelectedItem();
            if (selected != null) quickUpdateSuspectStatus(selected);
        });
        contextMenu.getItems().addAll(viewItem, editItem, linkItem, statusItem);

        suspectTable.setRowFactory(tv -> {
            TableRow<Suspect> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showSuspectDetails(row.getItem());
                }
            });
            row.setContextMenu(contextMenu);
            return row;
        });

        pane.setCenter(suspectTable);

        // --- BOTTOM ACTION BUTTON BAR ---
        HBox btnBox = new HBox(12);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getStyleClass().add(CLASS_ACTION_BAR);

        Button addBtn = new Button("Register Suspect");
        Button editBtn = new Button("Edit Selected Profile");
        Button linkBtn = new Button("Link to Case");
        Button statusBtn = new Button("Quick Status");
        Button viewBtn = new Button("View Dossier");

        addBtn.setOnAction(e -> openRegisterSuspectDialog());
        editBtn.setOnAction(e -> {
            Suspect selected = suspectTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a suspect from the table to edit.");
                return;
            }
            openEditSuspectDialog(selected);
        });
        linkBtn.setOnAction(e -> openLinkSuspectDialog());
        statusBtn.setOnAction(e -> {
            Suspect selected = suspectTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a suspect from the table first.");
                return;
            }
            quickUpdateSuspectStatus(selected);
        });
        viewBtn.setOnAction(e -> {
            Suspect selected = suspectTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a suspect from the table first.");
                return;
            }
            showSuspectDetails(selected);
        });

        btnBox.getChildren().addAll(addBtn, editBtn, linkBtn, statusBtn, viewBtn);
        pane.setBottom(btnBox);
        return pane;
    }

    private void openRegisterSuspectDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register Suspect Profile");
        dialog.setHeaderText("Enter suspect background details");
        ButtonType saveButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        TextField firstNameField = new TextField();
        TextField lastNameField = new TextField();
        DatePicker dobPicker = new DatePicker(LocalDate.of(1990, 1, 1));
        TextField traitsField = new TextField();
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Wanted", "In Custody", "Under Surveillance", "Cleared"));
        statusBox.setValue("Wanted");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Date of Birth:"), 0, 2);
        grid.add(dobPicker, 1, 2);
        grid.add(new Label("Physical Traits:"), 0, 3);
        grid.add(traitsField, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusBox, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        Runnable validate = () -> saveButton.setDisable(firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty());
        firstNameField.textProperty().addListener((obs, o, n) -> validate.run());
        lastNameField.textProperty().addListener((obs, o, n) -> validate.run());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String dob = dobPicker.getValue() != null ? dobPicker.getValue().toString() : "Unknown";
            String traits = traitsField.getText().trim().isEmpty() ? "None recorded" : traitsField.getText().trim();
            String id = "S-" + String.format("%04d", db.suspects.size() + 1);
            db.suspects.put(id, new Suspect(id, firstNameField.getText().trim(), lastNameField.getText().trim(), dob, traits, statusBox.getValue()));
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Suspect " + id + " registered.");
        }
    }

    /**
     * Comprehensive Edit Dialog for Suspects (Name, DOB, Traits, Status, and Aliases)
     */
    private void openEditSuspectDialog(Suspect s) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Suspect Profile");
        dialog.setHeaderText("Editing Suspect: " + s.getId() + " (" + s.getFirstName() + " " + s.getLastName() + ")");
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        TextField firstNameField = new TextField(s.getFirstName());
        TextField lastNameField = new TextField(s.getLastName());

        DatePicker dobPicker = new DatePicker();
        try {
            dobPicker.setValue(LocalDate.parse(s.getDateOfBirth()));
        } catch (Exception ex) {
            dobPicker.setValue(null);
        }

        TextField traitsField = new TextField(s.getPhysicalTraits());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList(
                "Wanted", "In Custody", "Under Surveillance", "Cleared"));
        statusBox.setValue(s.getStatus());

        // Aliases manager inside the edit dialog
        ListView<String> aliasListView = new ListView<>(FXCollections.observableArrayList(s.getAliases()));
        aliasListView.setPrefHeight(90);

        TextField newAliasField = new TextField();
        newAliasField.setPromptText("Add new alias...");
        Button addAliasBtn = new Button("+ Add");
        Button removeAliasBtn = new Button("Remove Selected");

        addAliasBtn.setOnAction(e -> {
            String alias = newAliasField.getText().trim();
            if (!alias.isEmpty() && !aliasListView.getItems().contains(alias)) {
                aliasListView.getItems().add(alias);
                newAliasField.clear();
            }
        });
        removeAliasBtn.setOnAction(e -> {
            String sel = aliasListView.getSelectionModel().getSelectedItem();
            if (sel != null) aliasListView.getItems().remove(sel);
        });

        HBox aliasInputBox = new HBox(8, newAliasField, addAliasBtn, removeAliasBtn);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Suspect ID:"), 0, 0);
        grid.add(new Label(s.getId()), 1, 0);
        grid.add(new Label("First Name:"), 0, 1);
        grid.add(firstNameField, 1, 1);
        grid.add(new Label("Last Name:"), 0, 2);
        grid.add(lastNameField, 1, 2);
        grid.add(new Label("Date of Birth:"), 0, 3);
        grid.add(dobPicker, 1, 3);
        grid.add(new Label("Physical Traits:"), 0, 4);
        grid.add(traitsField, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusBox, 1, 5);
        grid.add(new Label("Known Aliases:"), 0, 6);
        grid.add(new VBox(6, aliasListView, aliasInputBox), 1, 6);

        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable validate = () -> saveBtn.setDisable(firstNameField.getText().trim().isEmpty() || lastNameField.getText().trim().isEmpty());
        firstNameField.textProperty().addListener((o, a, b) -> validate.run());
        lastNameField.textProperty().addListener((o, a, b) -> validate.run());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            s.setFirstName(firstNameField.getText().trim());
            s.setLastName(lastNameField.getText().trim());
            s.setDateOfBirth(dobPicker.getValue() != null ? dobPicker.getValue().toString() : "Unknown");
            s.setPhysicalTraits(traitsField.getText().trim().isEmpty() ? "None recorded" : traitsField.getText().trim());
            s.setStatus(statusBox.getValue());

            // Synchronize aliases
            List<String> currentAliases = new ArrayList<>(s.getAliases());
            for (String a : currentAliases) s.removeAlias(a);
            for (String a : aliasListView.getItems()) s.addAlias(a);

            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Profile Updated", "Suspect [" + s.getId() + "] profile saved.");
        }
    }

    private void quickUpdateSuspectStatus(Suspect s) {
        String newStatus = promptChoice("Update Status", "New status for " + s.getId() + " (" + s.getFirstName() + " " + s.getLastName() + "):",
                Arrays.asList("Wanted", "In Custody", "Under Surveillance", "Cleared"), s.getStatus());
        if (newStatus != null) {
            s.setStatus(newStatus);
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Updated", s.getId() + " is now marked as " + newStatus + ".");
        }
    }

    private void openLinkSuspectDialog() {
        if (db.suspects.isEmpty() || db.cases.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Cannot Link", "You need at least one suspect and one case registered first.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Link Suspect to Case");
        dialog.setHeaderText("Choose a suspect and target case to link");
        ButtonType linkButtonType = new ButtonType("Link", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(linkButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        ComboBox<Suspect> suspectBox = new ComboBox<>(FXCollections.observableArrayList(db.suspects.values()));
        suspectBox.setConverter(displayConverter(s -> s.getId() + " - " + s.getFirstName() + " " + s.getLastName()));
        Suspect preselectedSuspect = suspectTable.getSelectionModel().getSelectedItem();
        if (preselectedSuspect != null) suspectBox.setValue(preselectedSuspect);

        ComboBox<Case> caseBox = new ComboBox<>(FXCollections.observableArrayList(db.cases.values()));
        caseBox.setConverter(displayConverter(c -> c.getCaseId() + " - " + c.getTitle()));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Suspect:"), 0, 0);
        grid.add(suspectBox, 1, 0);
        grid.add(new Label("Case File:"), 0, 1);
        grid.add(caseBox, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == linkButtonType) {
            Suspect s = suspectBox.getValue();
            Case c = caseBox.getValue();
            if (s == null || c == null) {
                showAlert(Alert.AlertType.ERROR, "Error", "Please select both a suspect and a case.");
                return;
            }
            if (s.getLinkedCaseIds().contains(c.getCaseId())) {
                showAlert(Alert.AlertType.WARNING, "Already Linked", s.getId() + " is already linked to " + c.getCaseId() + ".");
                return;
            }
            s.linkCase(c.getCaseId());
            c.addSuspect(s.getId());
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Success", s.getId() + " successfully linked to Case " + c.getCaseId() + ".");
        }
    }

    private void showSuspectDetails(Suspect s) {
        StringBuilder sb = new StringBuilder();
        sb.append("Suspect ID: ").append(s.getId()).append("\n");
        sb.append("Full Name: ").append(s.getFirstName()).append(" ").append(s.getLastName()).append("\n");
        sb.append("Date of Birth: ").append(s.getDateOfBirth()).append("\n");
        sb.append("Status: ").append(s.getStatus()).append("\n");
        sb.append("Physical Traits: ").append(s.getPhysicalTraits()).append("\n\n");

        sb.append("--- KNOWN ALIASES ---\n");
        if (s.getAliases().isEmpty()) sb.append("  (None recorded)\n");
        else for (String a : s.getAliases()) sb.append("  • ").append(a).append("\n");

        sb.append("\n--- LINKED INVESTIGATIONS ---\n");
        if (s.getLinkedCaseIds().isEmpty()) sb.append("  (No linked cases)\n");
        else {
            for (String cid : s.getLinkedCaseIds()) {
                Case c = db.cases.get(cid);
                String title = c != null ? c.getTitle() : "Unknown Case";
                sb.append("  • [").append(cid).append("] ").append(title).append("\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Suspect Intelligence Dossier");
        alert.setHeaderText(s.getId() + ": " + s.getFirstName() + " " + s.getLastName());
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(450, 320);
        alert.getDialogPane().setContent(area);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    /** =========================================================================
     * EVIDENCE LOCKER TAB
     * ========================================================================= */
    private BorderPane createEvidencePane() {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add(CLASS_CONTENT_PANE);

        // --- SEARCH + DROPDOWN FILTER BAR ---
        TextField searchField = new TextField();
        searchField.setPromptText("Search Evidence ID, Case ID, Description...");
        searchField.setPrefWidth(240);

        ComboBox<String> typeDropdown = new ComboBox<>(FXCollections.observableArrayList(
                "All Types", "Weapon", "Digital", "Document", "Biological"));
        typeDropdown.setValue("All Types");

        ComboBox<String> statusDropdown = new ComboBox<>(FXCollections.observableArrayList(
                "All Statuses", "In Storage", "At Lab/Forensics", "Released/Destroyed"));
        statusDropdown.setValue("All Statuses");

        Button clearFilterBtn = new Button("✕ Clear");
        clearFilterBtn.setOnAction(e -> {
            searchField.clear();
            typeDropdown.setValue("All Types");
            statusDropdown.setValue("All Statuses");
        });

        HBox filterBar = new HBox(10, new Label("Search:"), searchField, new Label("Type:"), typeDropdown,
                new Label("Status:"), statusDropdown, clearFilterBtn);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.setPadding(new Insets(10));
        filterBar.getStyleClass().add(CLASS_FILTER_BAR);
        pane.setTop(filterBar);

        // --- TABLE VIEW SETUP ---
        evidenceTable = new TableView<>();
        evidenceTable.setPlaceholder(new Label("No evidence matches current filters."));

        TableColumn<Evidence, String> idCol = new TableColumn<>("Evidence ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEvidenceId()));
        idCol.setPrefWidth(110);

        TableColumn<Evidence, String> caseCol = new TableColumn<>("Case ID");
        caseCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCaseId()));
        caseCol.setPrefWidth(110);

        TableColumn<Evidence, String> typeCol = new TableColumn<>("Category Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        typeCol.setPrefWidth(120);

        TableColumn<Evidence, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(280);

        TableColumn<Evidence, String> statusCol = new TableColumn<>("Status / Location");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));
        statusCol.setPrefWidth(150);

        evidenceTable.getColumns().addAll(idCol, caseCol, typeCol, descCol, statusCol);

        // Configure FilteredList & Sorting
        filteredEvidenceList = new FilteredList<>(masterEvidenceList, p -> true);
        Runnable updateFilter = () -> {
            String query = searchField.getText().toLowerCase().trim();
            String type = typeDropdown.getValue();
            String status = statusDropdown.getValue();
            filteredEvidenceList.setPredicate(ev -> {
                boolean matchesType = type == null || type.equals("All Types") || ev.getType().equalsIgnoreCase(type);
                boolean matchesStatus = status == null || status.equals("All Statuses") || ev.getStatus().equalsIgnoreCase(status);
                boolean matchesQuery = query.isEmpty() || ev.getEvidenceId().toLowerCase().contains(query)
                        || ev.getCaseId().toLowerCase().contains(query)
                        || ev.getDescription().toLowerCase().contains(query);
                return matchesType && matchesStatus && matchesQuery;
            });
        };
        searchField.textProperty().addListener((obs, o, n) -> updateFilter.run());
        typeDropdown.valueProperty().addListener((obs, o, n) -> updateFilter.run());
        statusDropdown.valueProperty().addListener((obs, o, n) -> updateFilter.run());

        SortedList<Evidence> sortedEvidence = new SortedList<>(filteredEvidenceList);
        sortedEvidence.comparatorProperty().bind(evidenceTable.comparatorProperty());
        evidenceTable.setItems(sortedEvidence);

        // --- CONTEXT MENU & DOUBLE CLICK ---
        ContextMenu contextMenu = new ContextMenu();
        MenuItem logItem = new MenuItem("View Chain of Custody");
        MenuItem editItem = new MenuItem("Edit Evidence Record");
        MenuItem statusItem = new MenuItem("Update Custody / Status");

        logItem.setOnAction(e -> {
            Evidence selected = evidenceTable.getSelectionModel().getSelectedItem();
            if (selected != null) showCustodyLog(selected);
        });
        editItem.setOnAction(e -> {
            Evidence selected = evidenceTable.getSelectionModel().getSelectedItem();
            if (selected != null) openEditEvidenceDialog(selected);
        });
        statusItem.setOnAction(e -> {
            Evidence selected = evidenceTable.getSelectionModel().getSelectedItem();
            if (selected != null) quickUpdateEvidenceStatus(selected);
        });
        contextMenu.getItems().addAll(logItem, editItem, statusItem);

        evidenceTable.setRowFactory(tv -> {
            TableRow<Evidence> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    showCustodyLog(row.getItem());
                }
            });
            row.setContextMenu(contextMenu);
            return row;
        });

        pane.setCenter(evidenceTable);

        // --- BOTTOM ACTION BUTTON BAR ---
        HBox btnBox = new HBox(12);
        btnBox.setPadding(new Insets(10));
        btnBox.setAlignment(Pos.CENTER);
        btnBox.getStyleClass().add(CLASS_ACTION_BAR);

        Button addBtn = new Button("Log New Evidence");
        Button editBtn = new Button("Edit Selected Item");
        Button custodyBtn = new Button("Update Custody/Status");
        Button logBtn = new Button("View Custody History");

        addBtn.setOnAction(e -> openLogEvidenceDialog());
        editBtn.setOnAction(e -> {
            Evidence selected = evidenceTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an evidence item from the table to edit.");
                return;
            }
            openEditEvidenceDialog(selected);
        });
        custodyBtn.setOnAction(e -> {
            Evidence selected = evidenceTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an evidence item from the table first.");
                return;
            }
            quickUpdateEvidenceStatus(selected);
        });
        logBtn.setOnAction(e -> {
            Evidence selected = evidenceTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an evidence item from the table first.");
                return;
            }
            showCustodyLog(selected);
        });

        btnBox.getChildren().addAll(addBtn, editBtn, custodyBtn, logBtn);
        pane.setBottom(btnBox);
        return pane;
    }

    private void openLogEvidenceDialog() {
        if (db.cases.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "No Cases", "You must create a case before logging evidence.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Log Evidence Item");
        dialog.setHeaderText("Enter evidence details and link to investigation");
        ButtonType saveButtonType = new ButtonType("Log Item", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        ComboBox<Case> caseBox = new ComboBox<>(FXCollections.observableArrayList(db.cases.values()));
        caseBox.setConverter(displayConverter(c -> c.getCaseId() + " - " + c.getTitle()));
        Case preselectedCase = caseTable != null ? caseTable.getSelectionModel().getSelectedItem() : null;
        if (preselectedCase != null) caseBox.setValue(preselectedCase);

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Weapon", "Digital", "Document", "Biological"));
        typeBox.setValue("Weapon");

        TextField descField = new TextField();
        descField.setPromptText("Detailed item description");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Associated Case:"), 0, 0);
        grid.add(caseBox, 1, 0);
        grid.add(new Label("Evidence Category:"), 0, 1);
        grid.add(typeBox, 1, 1);
        grid.add(new Label("Description:"), 0, 2);
        grid.add(descField, 1, 2);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        Runnable validate = () -> saveButton.setDisable(caseBox.getValue() == null || descField.getText().trim().isEmpty());
        caseBox.valueProperty().addListener((obs, o, n) -> validate.run());
        descField.textProperty().addListener((obs, o, n) -> validate.run());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            Case c = caseBox.getValue();
            String id = "EV-" + String.format("%04d", db.evidence.size() + 1);
            Evidence ev = new Evidence(id, c.getCaseId(), typeBox.getValue(), descField.getText().trim(), "In Storage");
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            ev.logCustody("Initial intake logged by Agent " + loggedInUser.getLastName() + " at " + time);
            db.evidence.put(id, ev);
            c.addEvidence(id);
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Evidence Logged", "Evidence " + id + " logged under Case " + c.getCaseId() + ".");
        }
    }

    /**
     * Comprehensive Edit Dialog for Evidence (Case assignment, Category, Description, Status)
     */
    private void openEditEvidenceDialog(Evidence ev) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Evidence Record");
        dialog.setHeaderText("Editing Evidence Item: " + ev.getEvidenceId());
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        ComboBox<Case> caseBox = new ComboBox<>(FXCollections.observableArrayList(db.cases.values()));
        caseBox.setConverter(displayConverter(c -> c.getCaseId() + " - " + c.getTitle()));
        Case currentCase = db.cases.get(ev.getCaseId());
        if (currentCase != null) caseBox.setValue(currentCase);

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Weapon", "Digital", "Document", "Biological"));
        typeBox.setValue(ev.getType());

        TextField descField = new TextField(ev.getDescription());

        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("In Storage", "At Lab/Forensics", "Released/Destroyed"));
        statusBox.setValue(ev.getStatus());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Evidence ID:"), 0, 0);
        grid.add(new Label(ev.getEvidenceId()), 1, 0);
        grid.add(new Label("Assigned Case:"), 0, 1);
        grid.add(caseBox, 1, 1);
        grid.add(new Label("Category Type:"), 0, 2);
        grid.add(typeBox, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descField, 1, 3);
        grid.add(new Label("Status / Location:"), 0, 4);
        grid.add(statusBox, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveButtonType);
        descField.textProperty().addListener((obs, o, n) -> saveBtn.setDisable(n.trim().isEmpty() || caseBox.getValue() == null));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String oldCaseId = ev.getCaseId();
            Case newCase = caseBox.getValue();

            // Check if reassigned to another case
            if (newCase != null && !newCase.getCaseId().equals(oldCaseId)) {
                ev.setCaseId(newCase.getCaseId());
                newCase.addEvidence(ev.getEvidenceId());
            }

            ev.setType(typeBox.getValue());
            ev.setDescription(descField.getText().trim());
            String oldStatus = ev.getStatus();
            String newStatus = statusBox.getValue();
            ev.setStatus(newStatus);

            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            ev.logCustody("Item edited (Status: " + oldStatus + " -> " + newStatus + ") by Agent " + loggedInUser.getLastName() + " at " + time);

            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Evidence Updated", "Record " + ev.getEvidenceId() + " updated successfully.");
        }
    }

    private void quickUpdateEvidenceStatus(Evidence ev) {
        String status = promptChoice("Update Custody Status", "New Status for " + ev.getEvidenceId() + ":",
                Arrays.asList("In Storage", "At Lab/Forensics", "Released/Destroyed"), ev.getStatus());
        if (status != null) {
            ev.setStatus(status);
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            ev.logCustody("Status -> " + status + " by Agent " + loggedInUser.getLastName() + " at " + time);
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Status Logged", ev.getEvidenceId() + " updated to " + status + ".");
        }
    }

    private void showCustodyLog(Evidence ev) {
        StringBuilder sb = new StringBuilder();
        sb.append("Evidence ID: ").append(ev.getEvidenceId()).append("\n");
        sb.append("Category: ").append(ev.getType()).append("\n");
        sb.append("Case: ").append(ev.getCaseId()).append("\n");
        sb.append("Description: ").append(ev.getDescription()).append("\n\n");
        sb.append("--- OFFICIAL CHAIN OF CUSTODY LOG ---\n");

        if (ev.getCustodyLog().isEmpty()) {
            sb.append("No custody movements recorded yet.\n");
        } else {
            for (String entry : ev.getCustodyLog()) {
                sb.append("• ").append(entry).append("\n");
            }
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chain of Custody Report");
        alert.setHeaderText("Evidence Item: " + ev.getEvidenceId());
        TextArea area = new TextArea(sb.toString());
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefSize(460, 300);
        alert.getDialogPane().setContent(area);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    /** =========================================================================
     * SEARCH & INTELLIGENCE TAB (GLOBAL MULTI-CRITERIA SEARCH)
     * ========================================================================= */
    private BorderPane createSearchPane() {
        BorderPane pane = new BorderPane();
        pane.getStyleClass().add(CLASS_CONTENT_PANE);

        searchDisplay = new TextArea();
        searchDisplay.setEditable(false);
        searchDisplay.setFont(Font.font("Monospaced", 13));
        searchDisplay.setWrapText(true);
        pane.setCenter(searchDisplay);

        // --- MULTI-CRITERIA SEARCH HEADER ---
        globalSearchField = new TextField();
        globalSearchField.setPromptText("Enter keyword, ID, name, or description...");
        globalSearchField.setPrefWidth(260);

        searchEntityFilter = new ComboBox<>(FXCollections.observableArrayList("All Categories", "Cases Only", "Suspects Only", "Evidence Only"));
        searchEntityFilter.setValue("All Categories");

        searchStatusFilter = new ComboBox<>(FXCollections.observableArrayList(
                "All Statuses", "Open", "Closed", "Cold", "Wanted", "In Custody", "Under Surveillance", "Cleared", "In Storage", "At Lab/Forensics"));
        searchStatusFilter.setValue("All Statuses");

        Button searchBtn = new Button("Search");
        Button clearBtn = new Button("✕ Clear");
        Button exportBtn = new Button("Export Dossier");

        Runnable runSearch = () -> {
            String q = globalSearchField.getText().toLowerCase().trim();
            String entity = searchEntityFilter.getValue();
            String status = searchStatusFilter.getValue();

            StringBuilder res = new StringBuilder();
            res.append("=======================================================================\n");
            res.append("                    FEDERAL REGISTRY INTELLIGENCE SEARCH\n");
            res.append("=======================================================================\n");
            res.append("Query: [").append(q.isEmpty() ? "ALL" : q).append("] | Target: [").append(entity).append("] | Filter Status: [").append(status).append("]\n");
            res.append("-----------------------------------------------------------------------\n\n");

            boolean foundAny = false;

            // Search Cases
            if (entity.equals("All Categories") || entity.equals("Cases Only")) {
                res.append("[CASE FILES]\n");
                boolean foundCase = false;
                for (Case c : db.cases.values()) {
                    boolean matchStatus = status.equals("All Statuses") || c.getStatus().equalsIgnoreCase(status);
                    boolean matchText = q.isEmpty() || c.getCaseId().toLowerCase().contains(q) || c.getTitle().toLowerCase().contains(q);
                    if (matchStatus && matchText) {
                        foundAny = true;
                        foundCase = true;
                        res.append("  • ").append(c.getCaseId()).append(" | ").append(c.getTitle())
                                .append(" | Status: ").append(c.getStatus()).append(" | Opened: ").append(c.getDateOpened()).append("\n");
                    }
                }
                if (!foundCase) res.append("  (No matching cases found)\n");
                res.append("\n");
            }

            // Search Suspects
            if (entity.equals("All Categories") || entity.equals("Suspects Only")) {
                res.append("[SUSPECT PROFILES]\n");
                boolean foundSuspect = false;
                for (Suspect s : db.suspects.values()) {
                    boolean matchStatus = status.equals("All Statuses") || s.getStatus().equalsIgnoreCase(status);
                    boolean matchText = q.isEmpty() || s.getId().toLowerCase().contains(q)
                            || s.getFirstName().toLowerCase().contains(q)
                            || s.getLastName().toLowerCase().contains(q)
                            || s.getPhysicalTraits().toLowerCase().contains(q);
                    if (matchStatus && matchText) {
                        foundAny = true;
                        foundSuspect = true;
                        res.append("  • ").append(s.getId()).append(" | ").append(s.getFirstName()).append(" ").append(s.getLastName())
                                .append(" | DOB: ").append(s.getDateOfBirth()).append(" | Status: ").append(s.getStatus())
                                .append(" | Traits: ").append(s.getPhysicalTraits()).append("\n");
                    }
                }
                if (!foundSuspect) res.append("  (No matching suspects found)\n");
                res.append("\n");
            }

            // Search Evidence
            if (entity.equals("All Categories") || entity.equals("Evidence Only")) {
                res.append("[LOGGED EVIDENCE]\n");
                boolean foundEvidence = false;
                for (Evidence ev : db.evidence.values()) {
                    boolean matchStatus = status.equals("All Statuses") || ev.getStatus().equalsIgnoreCase(status);
                    boolean matchText = q.isEmpty() || ev.getEvidenceId().toLowerCase().contains(q)
                            || ev.getCaseId().toLowerCase().contains(q)
                            || ev.getDescription().toLowerCase().contains(q)
                            || ev.getType().toLowerCase().contains(q);
                    if (matchStatus && matchText) {
                        foundAny = true;
                        foundEvidence = true;
                        res.append("  • ").append(ev.getEvidenceId()).append(" (Case ").append(ev.getCaseId()).append(") | Type: ")
                                .append(ev.getType()).append(" | Status: ").append(ev.getStatus()).append(" | Desc: ").append(ev.getDescription()).append("\n");
                    }
                }
                if (!foundEvidence) res.append("  (No matching evidence found)\n");
                res.append("\n");
            }

            if (!foundAny) {
                res.append(">>> No records matched your search criteria.\n");
            }
            searchDisplay.setText(res.toString());
        };

        searchBtn.setOnAction(e -> runSearch.run());
        globalSearchField.setOnAction(e -> runSearch.run());
        searchEntityFilter.setOnAction(e -> runSearch.run());
        searchStatusFilter.setOnAction(e -> runSearch.run());

        clearBtn.setOnAction(e -> {
            globalSearchField.clear();
            searchEntityFilter.setValue("All Categories");
            searchStatusFilter.setValue("All Statuses");
            runSearch.run();
        });

        exportBtn.setOnAction(e -> {
            String id = promptText("Export Case Dossier", "Enter target Case ID (e.g. C-2026-001):");
            if (id != null && db.cases.containsKey(id.trim())) {
                generateComprehensiveDossier(id.trim());
                showAlert(Alert.AlertType.INFORMATION, "Export Successful", "Comprehensive dossier exported to: Dossier_" + id.trim() + ".txt");
            } else if (id != null) {
                showAlert(Alert.AlertType.ERROR, "Not Found", "Case ID '" + id + "' was not found in registry.");
            }
        });

        HBox topBox = new HBox(8, new Label("Query:"), globalSearchField, new Label("Category:"), searchEntityFilter,
                new Label("Status:"), searchStatusFilter, searchBtn, clearBtn, exportBtn);
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(10));
        topBox.getStyleClass().add(CLASS_FILTER_BAR);
        pane.setTop(topBox);

        // Run an initial search to populate overview
        runSearch.run();

        return pane;
    }

    /** =========================================================================
     * ADMIN CONSOLE TAB
     * ========================================================================= */
    private VBox createAdminPane() {
        VBox pane = new VBox(14);
        pane.getStyleClass().add(CLASS_CONTENT_PANE);
        pane.setPadding(new Insets(18));

        Label warningLabel = new Label("ADMINISTRATOR CONSOLE — ACCESS RESTRICTED\nModify agent roles, register personnel, or purge classified records.");
        warningLabel.getStyleClass().add(CLASS_DANGER_TEXT);
        warningLabel.setTextAlignment(TextAlignment.CENTER);

        // --- AGENT ROSTER SEARCH & TABLE ---
        TextField agentSearchField = new TextField();
        agentSearchField.setPromptText("Filter agents by name, ID, or username...");
        agentSearchField.setPrefWidth(240);

        ComboBox<String> roleFilter = new ComboBox<>(FXCollections.observableArrayList("All Roles", "Admin", "Field Agent"));
        roleFilter.setValue("All Roles");

        HBox agentFilterBar = new HBox(10, new Label("Registered Agents:"), agentSearchField, new Label("Role:"), roleFilter);
        agentFilterBar.setAlignment(Pos.CENTER_LEFT);

        userTable = new TableView<>();
        userTable.setPrefHeight(160);

        TableColumn<User, String> uidCol = new TableColumn<>("Agent ID");
        uidCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        uidCol.setPrefWidth(100);

        TableColumn<User, String> unameCol = new TableColumn<>("Username");
        unameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        unameCol.setPrefWidth(140);

        TableColumn<User, String> ulastCol = new TableColumn<>("Last Name");
        ulastCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLastName()));
        ulastCol.setPrefWidth(160);

        TableColumn<User, String> uroleCol = new TableColumn<>("Access Role");
        uroleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));
        uroleCol.setPrefWidth(140);

        userTable.getColumns().addAll(uidCol, unameCol, ulastCol, uroleCol);

        // Filtered Agent Roster
        filteredUserList = new FilteredList<>(masterUserList, p -> true);
        Runnable updateAgentFilter = () -> {
            String q = agentSearchField.getText().toLowerCase().trim();
            String r = roleFilter.getValue();
            filteredUserList.setPredicate(u -> {
                boolean matchRole = r == null || r.equals("All Roles") || u.getRole().equalsIgnoreCase(r);
                boolean matchText = q.isEmpty() || u.getId().toLowerCase().contains(q)
                        || u.getUsername().toLowerCase().contains(q)
                        || u.getLastName().toLowerCase().contains(q);
                return matchRole && matchText;
            });
        };
        agentSearchField.textProperty().addListener((o, a, b) -> updateAgentFilter.run());
        roleFilter.valueProperty().addListener((o, a, b) -> updateAgentFilter.run());

        SortedList<User> sortedUsers = new SortedList<>(filteredUserList);
        sortedUsers.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedUsers);

        Button addUserBtn = new Button("Register New Agent");
        Button editUserBtn = new Button("Edit Selected Agent");

        addUserBtn.setOnAction(e -> openRegisterAgentDialog());
        editUserBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Select an agent from the roster first.");
                return;
            }
            openEditAgentDialog(selected);
        });

        HBox agentBtnBox = new HBox(10, addUserBtn, editUserBtn);

        // --- PERMANENT RECORD DELETION PANEL ---
        Label deleteLabel = new Label("Purge Classified Record from Database:");

        deleteTypeChoice = new ComboBox<>(FXCollections.observableArrayList("Case", "Suspect", "Evidence"));
        deleteTypeChoice.setValue("Case");
        deleteTypeChoice.setOnAction(e -> refreshDeleteList());

        deleteListView = new ListView<>();
        deleteListView.setPrefHeight(120);

        Button deleteBtn = new Button("Delete Selected Record");
        deleteBtn.getStyleClass().add(CLASS_DANGER_BUTTON);

        deleteBtn.setOnAction(e -> {
            String entry = deleteListView.getSelectionModel().getSelectedItem();
            if (entry == null) {
                showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a record from the deletion list first.");
                return;
            }
            String id = entry.split("\\|", 2)[0].trim();

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirm Permanent Purge");
            confirm.setHeaderText("Permanently delete " + id + "?");
            confirm.setContentText("Warning: This action is irreversible.\n" + entry);
            applyTheme(confirm.getDialogPane());

            Optional<ButtonType> choice = confirm.showAndWait();
            if (choice.isPresent() && choice.get() == ButtonType.OK) {
                boolean removed = db.cases.remove(id) != null
                        || db.suspects.remove(id) != null
                        || db.evidence.remove(id) != null;
                if (removed) {
                    refreshAllDisplays();
                    showAlert(Alert.AlertType.INFORMATION, "Purged", "Record " + id + " was permanently deleted.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Not Found", "Record could not be located.");
                }
            }
        });

        VBox deleteBox = new VBox(8, deleteLabel, new HBox(10, new Label("Category:"), deleteTypeChoice), deleteListView, deleteBtn);

        pane.getChildren().addAll(warningLabel, agentFilterBar, userTable, agentBtnBox, new Separator(), deleteBox);
        return pane;
    }

    private void openRegisterAgentDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Register New Personnel");
        dialog.setHeaderText("Create Agent Account");
        ButtonType saveButtonType = new ButtonType("Register", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        TextField userField = new TextField();
        userField.setPromptText("agent.username");
        PasswordField passField = new PasswordField();
        TextField lastField = new TextField();
        lastField.setPromptText("Last Name");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Field Agent", "Admin"));
        roleBox.setValue("Field Agent");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Username:"), 0, 0);
        grid.add(userField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passField, 1, 1);
        grid.add(new Label("Last Name:"), 0, 2);
        grid.add(lastField, 1, 2);
        grid.add(new Label("Assigned Role:"), 0, 3);
        grid.add(roleBox, 1, 3);
        dialog.getDialogPane().setContent(grid);

        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);
        Runnable validate = () -> saveButton.setDisable(
                userField.getText().trim().isEmpty() || passField.getText().isEmpty() || lastField.getText().trim().isEmpty());
        userField.textProperty().addListener((o, a, b) -> validate.run());
        passField.textProperty().addListener((o, a, b) -> validate.run());
        lastField.textProperty().addListener((o, a, b) -> validate.run());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            String id = "U-" + String.format("%03d", db.users.size() + 1);
            db.users.put(id, new User(id, "Agent", lastField.getText().trim(), "N/A",
                    userField.getText().trim(), passField.getText(), roleBox.getValue()));
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Agent " + lastField.getText().trim() + " added to registry.");
        }
    }

    /**
     * Edit Dialog for User / Agent accounts
     */
    private void openEditAgentDialog(User u) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Agent Profile");
        dialog.setHeaderText("Editing Personnel: " + u.getId() + " (" + u.getLastName() + ")");
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        applyTheme(dialog.getDialogPane());

        TextField userField = new TextField(u.getUsername());
        TextField lastField = new TextField(u.getLastName());
        PasswordField passField = new PasswordField();
        passField.setPromptText("Leave empty to keep unchanged");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Field Agent", "Admin"));
        roleBox.setValue(u.getRole());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.add(new Label("Agent ID:"), 0, 0);
        grid.add(new Label(u.getId()), 1, 0);
        grid.add(new Label("Username:"), 0, 1);
        grid.add(userField, 1, 1);
        grid.add(new Label("Last Name:"), 0, 2);
        grid.add(lastField, 1, 2);
        grid.add(new Label("Assigned Role:"), 0, 3);
        grid.add(roleBox, 1, 3);
        grid.add(new Label("Reset Password:"), 0, 4);
        grid.add(passField, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(saveButtonType);
        Runnable validate = () -> saveBtn.setDisable(userField.getText().trim().isEmpty() || lastField.getText().trim().isEmpty());
        userField.textProperty().addListener((o, a, b) -> validate.run());
        lastField.textProperty().addListener((o, a, b) -> validate.run());

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            u.setUsername(userField.getText().trim());
            u.setLastName(lastField.getText().trim());
            u.setRole(roleBox.getValue());
            if (!passField.getText().isEmpty()) {
                u.setPassword(passField.getText());
            }
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Updated", "Agent profile " + u.getId() + " updated.");
        }
    }

    private void refreshDeleteList() {
        if (deleteTypeChoice == null || deleteListView == null) return;
        ObservableList<String> items = FXCollections.observableArrayList();
        String type = deleteTypeChoice.getValue();
        if ("Case".equals(type)) {
            for (Case c : db.cases.values()) {
                items.add(c.getCaseId() + " | " + c.getTitle() + " (" + c.getStatus() + ")");
            }
        } else if ("Suspect".equals(type)) {
            for (Suspect s : db.suspects.values()) {
                items.add(s.getId() + " | " + s.getFirstName() + " " + s.getLastName() + " (" + s.getStatus() + ")");
            }
        } else if ("Evidence".equals(type)) {
            for (Evidence ev : db.evidence.values()) {
                items.add(ev.getEvidenceId() + " | " + ev.getType() + " - " + ev.getDescription() + " (Case " + ev.getCaseId() + ")");
            }
        }
        deleteListView.setItems(items);
    }

    /** =========================================================================
     * CORE REFRESH & SYNCHRONIZATION
     * ========================================================================= */
    private void refreshAllDisplays() {
        masterCaseList.setAll(db.cases.values());
        masterSuspectList.setAll(db.suspects.values());
        masterEvidenceList.setAll(db.evidence.values());
        masterUserList.setAll(db.users.values());
        refreshDeleteList();
    }

    private void generateComprehensiveDossier(String caseId) {
        Case c = db.cases.get(caseId);
        if (c == null) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("Dossier_" + caseId + ".txt"))) {
            bw.write("=======================================================================\n");
            bw.write("                  OFFICIAL CASE INTELLIGENCE DOSSIER\n");
            bw.write("=======================================================================\n");
            bw.write("Case ID: " + c.getCaseId() + "\nTitle: " + c.getTitle() + "\nStatus: " + c.getStatus() + "\nDate Opened: " + c.getDateOpened() + "\n\n");

            bw.write("--- LINKED SUSPECTS ---\n");
            if (c.getSuspectIds().isEmpty()) {
                bw.write("(None linked)\n");
            } else {
                for (String sid : c.getSuspectIds()) {
                    Suspect s = db.suspects.get(sid);
                    if (s != null) {
                        bw.write("• [" + s.getId() + "] " + s.getFirstName() + " " + s.getLastName() + " (Status: " + s.getStatus() + ")\n");
                    }
                }
            }

            bw.write("\n--- RECOVERED EVIDENCE ---\n");
            boolean anyEvidence = false;
            for (Evidence ev : db.evidence.values()) {
                if (ev.getCaseId().equals(caseId)) {
                    anyEvidence = true;
                    bw.write("• [" + ev.getEvidenceId() + "] Type: " + ev.getType() + " | Status: " + ev.getStatus() + " | Desc: " + ev.getDescription() + "\n");
                }
            }
            if (!anyEvidence) bw.write("(None logged)\n");
            bw.write("\n======================= END OF CLASSIFIED REPORT ======================\n");
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
        applyTheme(dialog.getDialogPane());
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private String promptChoice(String title, String header, List<String> choices, String defaultChoice) {
        ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        applyTheme(dialog.getDialogPane());
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        applyTheme(alert.getDialogPane());
        alert.showAndWait();
    }

    private void applyTheme(DialogPane pane) {
        if (stylesheetUrl != null) {
            pane.getStylesheets().add(stylesheetUrl);
        }
    }

    private String getStylesheetUrl() {
        URL url = FederalRegistryFX.class.getResource("style.css");
        return url != null ? url.toExternalForm() : null;
    }

    private <T> StringConverter<T> displayConverter(Function<T, String> displayFn) {
        return new StringConverter<T>() {
            @Override
            public String toString(T obj) {
                return obj == null ? "" : displayFn.apply(obj);
            }

            @Override
            public T fromString(String string) {
                throw new UnsupportedOperationException("Not supported for non-editable dropdowns.");
            }
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}