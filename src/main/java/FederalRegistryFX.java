import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
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
import java.util.function.Consumer;
import java.util.function.Function;

public class FederalRegistryFX extends Application {
    private DataManager db = new DataManager();
    private User loggedInUser = null;

    private Stage primaryStage;
//    private Scene loginScene, dashboardScene;
// Replace the old Scene fields with Pane fields:
    private Parent loginPane;
    private Parent dashboardPane;
    private String stylesheetUrl;

    // CSS Classes
    private static final String CLASS_HEADER_BAR = "header-bar", CLASS_CONTENT_PANE = "content-pane",
                                CLASS_WELCOME_LABEL = "welcome-label", CLASS_LOGIN_TITLE = "login-title",
                                CLASS_DANGER_TEXT = "danger-text", CLASS_DANGER_BUTTON = "danger-button",
                                CLASS_FILTER_BAR = "filter-bar", CLASS_ACTION_BAR = "action-bar";

    // Collections
    private final ObservableList<Case> masterCaseList = FXCollections.observableArrayList();
    private final ObservableList<Suspect> masterSuspectList = FXCollections.observableArrayList();
    private final ObservableList<Evidence> masterEvidenceList = FXCollections.observableArrayList();
    private final ObservableList<User> masterUserList = FXCollections.observableArrayList();

    // UI Elements
    private TableView<Case> caseTable;
    private TableView<Suspect> suspectTable;
    private TableView<Evidence> evidenceTable;
    private TableView<User> userTable;
    private Label welcomeLabel;
    private TabPane tabPane;
    private Tab adminTab;
    private ComboBox<String> deleteTypeChoice;
    private ListView<String> deleteListView;

    // --- HOME DASHBOARD CONTROLS ---
    private Label homeWelcomeLabel;
    private Label homeCaseCountLabel;
    private Label homeSuspectCountLabel;
    private Label homeEvidenceCountLabel;
    private ListView<String> homeRecentCasesList;
    private ListView<String> homeWantedSuspectsList;

//    @Override
//    public void start(Stage primaryStage) {
//        this.primaryStage = primaryStage;
//        db.loadData();
//        if (db.users.isEmpty()) db.users.put("U-001", new User("U-001", "System", "Admin", "1980-01-01", "admin", "admin123", "Admin"));
//        stylesheetUrl = getStylesheetUrl();
//
//        primaryStage.setTitle("Federal Evidence & Suspect Registry");
//        primaryStage.setOnCloseRequest(e -> { db.saveData(); System.exit(0); });
//
//        loginPane = new Scene(createLoginPane(), 920, 680);
//        dashboardScene = new Scene(createDashboardPane(), 960, 700);
//
//        applyTheme(loginScene.getRoot());
//        applyTheme(dashboardScene.getRoot());
//
//        primaryStage.setScene(loginScene);
//        primaryStage.show();
//    }

//    /** ================== LOGIN ================== */
//    private GridPane createLoginPane() {
//        GridPane grid = createFormGrid();
//        grid.setAlignment(Pos.CENTER);
//
//        Label title = new Label("FEDERAL REGISTRY AUTHENTICATION");
//        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
//        title.getStyleClass().add(CLASS_LOGIN_TITLE);
//
//        TextField userField = new TextField(); userField.setPromptText("Enter agent username");
//        PasswordField pwBox = new PasswordField(); pwBox.setPromptText("Enter secure password");
//        Label errorLabel = new Label(); errorLabel.getStyleClass().add(CLASS_DANGER_TEXT);
//
//        Button btn = new Button("Authenticate"); btn.setDefaultButton(true);
//        btn.setOnAction(e -> authenticate(userField, pwBox, errorLabel));
//
//        grid.add(title, 0, 0, 2, 1);
//        grid.addRow(1, new Label("Username:"), userField);
//        grid.addRow(2, new Label("Password:"), pwBox);
//        grid.add(errorLabel, 0, 3, 2, 1);
//        grid.add(new HBox(btn) {{ setAlignment(Pos.BOTTOM_RIGHT); }}, 1, 4);
//
//        return grid;
//    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        db = new DataManager();
        db.loadData();

        if (db.users.isEmpty()) db.users.put("U-001", new User("U-001", "System", "Admin", "1980-01-01", "admin", "admin123", "Admin"));
        stylesheetUrl = getStylesheetUrl();

        primaryStage.setTitle("Federal Evidence & Suspect Registry");

        primaryStage.setOnCloseRequest(e -> {
            db.saveData();
            System.exit(0);
        });

        // 1. Create the two root views
        loginPane = createLoginPane();
        dashboardPane = createDashboardPane();

        // 2. Initialize ONE single Scene with loginPane
        Scene mainScene = new Scene(loginPane, 960, 700);
        if (stylesheetUrl != null) {
            mainScene.getStylesheets().add(stylesheetUrl);
        }

        primaryStage.setScene(mainScene);
        primaryStage.show();
    }
    /** ==================== RESPONSIVE LOGIN PANE ==================== */
    private StackPane createLoginPane() {
        StackPane root = new StackPane();
        root.getStyleClass().add("login-root");

        // Centered Authentication Card
        VBox loginCard = new VBox(16);
        // FIX: Set max width AND lock max height so it does NOT stretch top-to-bottom on fullscreen
        loginCard.setMaxSize(440, Region.USE_PREF_SIZE);
        loginCard.setAlignment(Pos.CENTER);
        loginCard.setPadding(new Insets(32, 36, 28, 36));
        loginCard.getStyleClass().add("login-card");

        // 1. Top Security Badge & Titles
        Label badgeLabel = new Label("RESTRICTED ACCESS PORTAL");
        badgeLabel.getStyleClass().add("login-badge");

        Label title = new Label("FEDERAL REGISTRY");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.getStyleClass().add("login-title");

        Label subtitle = new Label("Enter authorized credentials to unlock intelligence database");
        subtitle.setStyle("-fx-text-fill: #7f848e; -fx-font-size: 12px;");
        subtitle.setWrapText(true);
        subtitle.setTextAlignment(TextAlignment.CENTER);

        VBox headerBox = new VBox(6, badgeLabel, title, subtitle);
        headerBox.setAlignment(Pos.CENTER);

        // 2. Form Inputs
        VBox formBox = new VBox(12);
        formBox.setAlignment(Pos.CENTER_LEFT);

        Label userLabel = new Label("Agent Username");
        userLabel.getStyleClass().add("form-field-label");

        TextField userField = new TextField();
        userField.setPromptText("e.g. admin");
        userField.getStyleClass().add("login-input");

        Label pwLabel = new Label("Security Passcode");
        pwLabel.getStyleClass().add("form-field-label");

        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("••••••••••••");
        pwBox.getStyleClass().add("login-input");

        formBox.getChildren().addAll(userLabel, userField, pwLabel, pwBox);

        // 3. Error Feedback Label
        Label errorLabel = new Label();
        errorLabel.getStyleClass().add(CLASS_DANGER_TEXT);
        errorLabel.setWrapText(true);
        errorLabel.setTextAlignment(TextAlignment.CENTER);

        // 4. Authenticate Button
        Button btn = new Button("Authenticate Agent →");
        btn.setDefaultButton(true);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("login-button");
        btn.setOnAction(e -> authenticate(userField, pwBox, errorLabel)
        );

        // 5. Official Footer
        Label footerNotice = new Label("🔒 Official Federal Investigation Network\nUnauthorized access attempts are monitored and logged.");
        footerNotice.getStyleClass().add("login-footer-text");
        footerNotice.setTextAlignment(TextAlignment.CENTER);

        loginCard.getChildren().addAll(headerBox, formBox, errorLabel, btn, new Separator(), footerNotice);

        root.getChildren().add(loginCard);
        StackPane.setAlignment(loginCard, Pos.CENTER);
        return root;
    }

    private void authenticate(TextField userField, PasswordField pwBox, Label errorLabel) {
        try {
            // VALIDATE CREDENTIALS FIRST
            validateAgentCredentials(userField.getText(), pwBox.getText());
        for (User u : db.users.values()) {
            if (u.getUsername().equalsIgnoreCase(userField.getText().trim()) && u.getPassword().equals(pwBox.getText())) {
                loggedInUser = u;
                welcomeLabel.setText("Active Agent: " + u.getLastName() + " | Role: " + u.getRole());
                // 1. Reset active tab to HOME (index 0)
                tabPane.getSelectionModel().select(0);

                // 2. Dynamically show or hide the Admin Console tab
                if (u.getRole().equalsIgnoreCase("Admin")) {
                    if (!tabPane.getTabs().contains(adminTab)) {
                        tabPane.getTabs().add(adminTab); // Appends Admin Console at the end
                    }
                } else {
                    tabPane.getTabs().remove(adminTab); // Completely hides Admin Console for Field Agents
                }
                refreshAllDisplays();
                primaryStage.getScene().setRoot(dashboardPane);
              //  primaryStage.setScene(dashboardScene);
                userField.clear(); pwBox.clear(); errorLabel.setText("");
                return;
            }
        }
        errorLabel.setText("Access Denied: Invalid agent credentials.");
        showAlert(Alert.AlertType.ERROR, "Authentication Error", "Access Denied. Invalid credentials.");

        } catch (RegistryValidationException e) {
            // 4. DISPLAY THE CUSTOM EXCEPTION MESSAGE ON SCREEN
            errorLabel.setText(e.getMessage());
            showAlert(Alert.AlertType.WARNING, "Validation Error", e.getMessage());
        }
    }

    /** ================== DASHBOARD & TABS ================== */
    private BorderPane createDashboardPane() {
        BorderPane borderPane = new BorderPane();
        BorderPane header = new BorderPane();
        header.getStyleClass().add(CLASS_HEADER_BAR);

        welcomeLabel = new Label(); welcomeLabel.getStyleClass().add(CLASS_WELCOME_LABEL);
        Button logoutBtn = new Button("Logout"); logoutBtn.getStyleClass().add(CLASS_DANGER_BUTTON);
        logoutBtn.setOnAction(e -> {
            loggedInUser = null;
            // 1. Reset selection to the first tab (HOME)
            tabPane.getSelectionModel().select(0);
            // 2. Remove Admin Console tab on logout so it's clean for the next user
            tabPane.getTabs().remove(adminTab);

            primaryStage.getScene().setRoot(loginPane); });
            //primaryStage.setScene(loginScene); });


        BorderPane.setMargin(logoutBtn,new Insets(15,20,0,0) );
        BorderPane.setMargin(welcomeLabel,new Insets(8,0,8,30) );
        header.setLeft(welcomeLabel); header.setRight(logoutBtn);
        borderPane.setTop(header);

        tabPane = new TabPane(); tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        adminTab = new Tab("Admin Console", createAdminPane());

        tabPane.getTabs().addAll(
                new Tab("HOME", createHomePane()),
                new Tab("Case Management", createCasePane()),
                new Tab("Suspect Registry", createSuspectPane()),
                new Tab("Evidence Locker", createEvidencePane()),
                adminTab
        );
        borderPane.setCenter(tabPane);
        return borderPane;
    }

//    /** ================== HOME PAGE ================== */
//    private BorderPane createHomePane() {
//        BorderPane homePane = new BorderPane();
//        homePane.setPadding(new Insets(15));
//
//        String userInfo;
//        if (loggedInUser != null) {
//            userInfo = "Welcome, " + loggedInUser.getLastName() + " (" + loggedInUser.getRole() + ")";
//        } else {
//            userInfo = "Welcome to the Federal Registry System";
//        }
//
//        Label welcome = new Label(userInfo);
//        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 18));
//        homePane.setTop(welcome);
//        BorderPane.setAlignment(welcome, Pos.CENTER);
//
//        HBox statsBox = new HBox(20);
//        statsBox.setAlignment(Pos.CENTER);
//        Label caseCount = new Label("Cases: " + db.cases.size());
//        Label suspectCount = new Label("Suspects: " + db.suspects.size());
//        Label evidenceCount = new Label("Evidence: " + db.evidence.size());
//        statsBox.getChildren().addAll(caseCount, suspectCount, evidenceCount);
//        homePane.setCenter(statsBox);
//        return homePane;
//    }
    /** ==================== UPGRADED HOME PAGE ====================
       ==================== RESPONSIVE HOME PANE ==================== */
    private BorderPane createHomePane() {
        BorderPane homePane = new BorderPane();
        homePane.setPadding(new Insets(20));
        homePane.getStyleClass().add(CLASS_CONTENT_PANE);

        // 1. TOP HEADER
        VBox headerBox = new VBox(4);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(0, 0, 14, 0));

        homeWelcomeLabel = new Label("Welcome to the Federal Registry System");
        homeWelcomeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        homeWelcomeLabel.getStyleClass().add(CLASS_LOGIN_TITLE);

        Label subHeader = new Label("System Status: OPERATIONAL  •  Clearance Level: CONFIDENTIAL  •  Network: SECURE");
        subHeader.setStyle("-fx-text-fill: #7f848e; -fx-font-size: 12px;");
        headerBox.getChildren().addAll(homeWelcomeLabel, subHeader);
        homePane.setTop(headerBox);

        // 2. CENTER CONTENT (Constrained to 1100px max width for great widescreen look)
        VBox dashboardContent = new VBox(18);
        dashboardContent.setMaxWidth(1100);
        dashboardContent.setAlignment(Pos.TOP_CENTER);
        dashboardContent.setPadding(new Insets(6));

        // --- 3 Responsive Stat Cards ---
        HBox statsBox = new HBox(16);
        statsBox.setAlignment(Pos.CENTER);

        // Card 1: Cases
        VBox caseCard = new VBox(6);
        caseCard.getStyleClass().addAll("stat-card", "stat-card-cases");
        caseCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(caseCard, Priority.ALWAYS); // Stretches proportionally
        Label caseTitle = new Label("📁 ACTIVE CASES");
        caseTitle.getStyleClass().add("stat-card-title");
        homeCaseCountLabel = new Label(String.valueOf(db.cases.size()));
        homeCaseCountLabel.getStyleClass().addAll("stat-card-number", "stat-number-cases");
        Label caseSub = new Label("Click to view cases →");
        caseSub.getStyleClass().add("stat-card-sub");
        caseCard.getChildren().addAll(caseTitle, homeCaseCountLabel, caseSub);
        caseCard.setOnMouseClicked(e -> tabPane.getSelectionModel().select(1));

        // Card 2: Suspects
        VBox suspectCard = new VBox(6);
        suspectCard.getStyleClass().addAll("stat-card", "stat-card-suspects");
        suspectCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(suspectCard, Priority.ALWAYS); // Stretches proportionally
        Label suspectTitle = new Label("👤 TRACKED SUSPECTS");
        suspectTitle.getStyleClass().add("stat-card-title");
        homeSuspectCountLabel = new Label(String.valueOf(db.suspects.size()));
        homeSuspectCountLabel.getStyleClass().addAll("stat-card-number", "stat-number-suspects");
        Label suspectSub = new Label("Click to view suspects →");
        suspectSub.getStyleClass().add("stat-card-sub");
        suspectCard.getChildren().addAll(suspectTitle, homeSuspectCountLabel, suspectSub);
        suspectCard.setOnMouseClicked(e -> tabPane.getSelectionModel().select(2));

        // Card 3: Evidence
        VBox evidenceCard = new VBox(6);
        evidenceCard.getStyleClass().addAll("stat-card", "stat-card-evidence");
        evidenceCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(evidenceCard, Priority.ALWAYS); // Stretches proportionally
        Label evidenceTitle = new Label("📦 LOGGED EVIDENCE");
        evidenceTitle.getStyleClass().add("stat-card-title");
        homeEvidenceCountLabel = new Label(String.valueOf(db.evidence.size()));
        homeEvidenceCountLabel.getStyleClass().addAll("stat-card-number", "stat-number-evidence");
        Label evidenceSub = new Label("Click to view evidence →");
        evidenceSub.getStyleClass().add("stat-card-sub");
        evidenceCard.getChildren().addAll(evidenceTitle, homeEvidenceCountLabel, evidenceSub);
        evidenceCard.setOnMouseClicked(e -> tabPane.getSelectionModel().select(3));

        statsBox.getChildren().addAll(caseCard, suspectCard, evidenceCard);

        // --- Quick Action Bar ---
        HBox actionBox = new HBox(14);
        actionBox.setAlignment(Pos.CENTER);
        actionBox.setPadding(new Insets(12));
        actionBox.setMaxWidth(Double.MAX_VALUE);
        actionBox.getStyleClass().add("home-action-bar");

        Label quickLbl = new Label("Quick Actions:");
        //quickLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #abb2bf; -fx-font-size: 13px;");
        quickLbl.getStyleClass().add(CLASS_DANGER_TEXT);
        Button btnCase = new Button("+ New Case");
        btnCase.setOnAction(e -> openCreateCaseDialog());

        Button btnSuspect = new Button("+ Register Suspect");
        btnSuspect.setOnAction(e -> openRegisterSuspectDialog());

        Button btnEvidence = new Button("+ Log Evidence");
        btnEvidence.setOnAction(e -> openLogEvidenceDialog());

        actionBox.getChildren().addAll(quickLbl, btnCase, btnSuspect, btnEvidence);

        // --- Lower Split Activity Feeds ---
        HBox feedsBox = new HBox(16);
        feedsBox.setAlignment(Pos.CENTER);
        feedsBox.setMaxWidth(Double.MAX_VALUE);

        // Active Cases Panel
        VBox recentCasesBox = new VBox(8);
        recentCasesBox.getStyleClass().add("home-section-card");
        recentCasesBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(recentCasesBox, Priority.ALWAYS); // Stretches proportionally
        Label recentCasesHeader = new Label("⚡ Active Case Files");
        recentCasesHeader.getStyleClass().add("home-section-header");
        homeRecentCasesList = new ListView<>();
        homeRecentCasesList.setPrefHeight(180);
        homeRecentCasesList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && homeRecentCasesList.getSelectionModel().getSelectedItem() != null) {
                String item = homeRecentCasesList.getSelectionModel().getSelectedItem();
                String id = item.split("\\|", 2)[0].trim();
                Case c = db.cases.get(id);
                if (c != null) showCaseDetails(c);
            }
        });
        recentCasesBox.getChildren().addAll(recentCasesHeader, homeRecentCasesList);

        // Wanted Suspects Panel
        VBox wantedBox = new VBox(8);
        wantedBox.getStyleClass().add("home-section-card");
        wantedBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wantedBox, Priority.ALWAYS); // Stretches proportionally
        Label wantedHeader = new Label("🚨 Wanted Persons of Interest");
        wantedHeader.getStyleClass().add("home-section-header");
        homeWantedSuspectsList = new ListView<>();
        homeWantedSuspectsList.setPrefHeight(180);
        homeWantedSuspectsList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && homeWantedSuspectsList.getSelectionModel().getSelectedItem() != null) {
                String item = homeWantedSuspectsList.getSelectionModel().getSelectedItem();
                String id = item.split("\\|", 2)[0].trim();
                Suspect s = db.suspects.get(id);
                if (s != null) showSuspectDetails(s);
            }
        });
        wantedBox.getChildren().addAll(wantedHeader, homeWantedSuspectsList);

        feedsBox.getChildren().addAll(recentCasesBox, wantedBox);

        dashboardContent.getChildren().addAll(statsBox, actionBox, feedsBox);

        // Outer center wrapper to ensure the 1100px dashboard stays centered on 1080p/4k screens
        StackPane centerWrapper = new StackPane(dashboardContent);
        centerWrapper.setAlignment(Pos.TOP_CENTER);

        ScrollPane scrollPane = new ScrollPane(centerWrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        homePane.setCenter(scrollPane);
        return homePane;
    }

    private void updateHomeStats() {
        if (homeCaseCountLabel == null) return;
        // 1. Update Welcome Message with active agent name
        if (loggedInUser != null) {
            homeWelcomeLabel.setText("Welcome, Agent " + loggedInUser.getLastName() + " (" + loggedInUser.getRole() + ")");
        } else {
            homeWelcomeLabel.setText("Welcome to the Federal Registry System");
        }

        // 2. Update KPI numbers
        homeCaseCountLabel.setText(String.valueOf(db.cases.size()));
        homeSuspectCountLabel.setText(String.valueOf(db.suspects.size()));
        homeEvidenceCountLabel.setText(String.valueOf(db.evidence.size()));

        // 3. Populate Active Cases Feed
        ObservableList<String> casesList = FXCollections.observableArrayList();
        for (Case c : db.cases.values()) {
            if ("Open".equalsIgnoreCase(c.getStatus())) {
                casesList.add(c.getCaseId() + " | " + c.getTitle());
            }
        }
        if (casesList.isEmpty()) casesList.add("No open cases currently.");
        homeRecentCasesList.setItems(casesList);

        // 4. Populate Wanted Suspects Feed
        ObservableList<String> wantedList = FXCollections.observableArrayList();
        for (Suspect s : db.suspects.values()) {
            if ("Wanted".equalsIgnoreCase(s.getStatus())) {
                wantedList.add(s.getId() + " | " + s.getFirstName() + " " + s.getLastName());
            }
        }
        if (wantedList.isEmpty()) wantedList.add("No wanted suspects recorded.");
        homeWantedSuspectsList.setItems(wantedList);
    }

    /** ================== CASE MANAGEMENT ================== */
    @SuppressWarnings("unchecked")
    private BorderPane createCasePane() {
        BorderPane pane = new BorderPane(); pane.getStyleClass().add(CLASS_CONTENT_PANE);

        TextField searchField = new TextField(); searchField.setPromptText("Search Cases...");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "Open", "Closed", "Cold"));
        statusBox.setValue("All Statuses");

        pane.setTop(createFilterBar(searchField, statusBox, () -> { searchField.clear(); statusBox.setValue("All Statuses"); }));

        caseTable = new TableView<>();
        caseTable.getColumns().addAll(
                createCol("Case ID", 120, Case::getCaseId), createCol("Case Title", 340, Case::getTitle),
                createCol("Date Opened", 130, Case::getDateOpened), createCol("Status", 120, Case::getStatus)
        );

        FilteredList<Case> filtered = new FilteredList<>(masterCaseList, p -> true);
        Runnable filterLogic = () -> applyFilter(filtered, searchField.getText(),
                c -> statusBox.getValue().equals("All Statuses") || c.getStatus().equalsIgnoreCase(statusBox.getValue()),
                c -> c.getCaseId() + " " + c.getTitle() + " " + c.getDateOpened());

        searchField.textProperty().addListener(o -> filterLogic.run());
        statusBox.valueProperty().addListener(o -> filterLogic.run());
        caseTable.setItems(bindSort(filtered, caseTable));

        setupTableInteractions(caseTable, this::showCaseDetails,
                createMenuItem("View Details", e -> runIfSelected(caseTable, this::showCaseDetails)),
                createMenuItem("Edit Case Record", e -> runIfSelected(caseTable, this::openEditCaseDialog)),
                createMenuItem("Quick Status Update", e -> runIfSelected(caseTable, this::quickUpdateCaseStatus))
        );
        pane.setCenter(caseTable);

        pane.setBottom(createActionBar(
                createButton("Create New Case", e -> openCreateCaseDialog()),
                createButton("Edit Selected Case", e -> requireSelection(caseTable, this::openEditCaseDialog)),
                createButton("Quick Status", e -> requireSelection(caseTable, this::quickUpdateCaseStatus)),
                createButton("View Full Details", e -> requireSelection(caseTable, this::showCaseDetails)),
                createButton("Export Dossier", e -> exportDossier())
        ));
        return pane;
    }

    public void exportDossier(){
        String id = promptText();
        if (id != null && db.cases.containsKey(id.trim())) { generateDossier(id.trim()); showAlert(Alert.AlertType.INFORMATION, "Exported", "Dossier exported to Dossier_" + id.trim() + ".txt"); }
        else if (id != null) showAlert(Alert.AlertType.ERROR, "Not Found", "Case ID not found.");
    }

    private void openCreateCaseDialog() {
        Dialog<ButtonType> dialog = buildDialog("Create Investigation Case", "Enter initial case details", "Create Case");
        TextField titleField = new TextField(); titleField.setPromptText("e.g., Operation Nightfall");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Open", "Closed", "Cold"));
        statusBox.setValue("Open");

        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Case Title:"), titleField);
        grid.addRow(1, new Label("Initial Status:"), statusBox);
        dialog.getDialogPane().setContent(grid);

        Node createBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        createBtn.setDisable(true); titleField.textProperty().addListener((o, old, newVal) -> createBtn.setDisable(newVal.trim().isEmpty()));

        // NEW (100% null-safe):
        if (showAndConfirm(dialog)) {
            String id = "C-" + LocalDate.now().getYear() + "-" + String.format("%03d", db.cases.size() + 1);
            db.cases.put(id, new Case(id, titleField.getText().trim(), LocalDate.now().toString(), statusBox.getValue()));
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Case Created", "Case [" + id + "] has been registered.");
        }
    }

    private void openEditCaseDialog(Case c) {
        Dialog<ButtonType> dialog = buildDialog("Edit Case Details", "Editing Case: " + c.getCaseId(), "Save Changes");
        TextField titleField = new TextField(c.getTitle());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Open", "Closed", "Cold"));
        statusBox.setValue(c.getStatus());

        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Case ID:"), new Label(c.getCaseId() + " (Opened: " + c.getDateOpened() + ")"));
        grid.addRow(1, new Label("Case Title:"), titleField);
        grid.addRow(2, new Label("Case Status:"), statusBox);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        titleField.textProperty().addListener((o, old, newVal) -> saveBtn.setDisable(newVal.trim().isEmpty()));

        // NEW (100% null-safe):
        if (showAndConfirm(dialog)) {
            c.setTitle(titleField.getText().trim()); c.setStatus(statusBox.getValue());
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Case Updated", "Changes saved successfully.");
        }
    }

    private void quickUpdateCaseStatus(Case c) {
        String status = promptChoice("Select new status for " + c.getCaseId() + ":", Arrays.asList("Open", "Closed", "Cold"), c.getStatus());
        if (status != null) { c.setStatus(status); refreshAllDisplays(); }
    }

    private void showCaseDetails(Case c) {
        StringBuilder sb = new StringBuilder("Case ID: ").append(c.getCaseId()).append("\nTitle: ").append(c.getTitle())
                .append("\nStatus: ").append(c.getStatus()).append("\nOpened: ").append(c.getDateOpened()).append("\n\n--- LINKED SUSPECTS ---\n");
        c.getSuspectIds().forEach(sid -> { Suspect s = db.suspects.get(sid); if (s != null) sb.append("  • [").append(sid).append("] ").append(s.getFirstName()).append(" ").append(s.getLastName()).append(" (Status: ").append(s.getStatus()).append(")\n"); });
        if (c.getSuspectIds().isEmpty()) sb.append("  (No suspects currently linked)\n");
        sb.append("\n--- LOGGED EVIDENCE ---\n");
        db.evidence.values().stream().filter(ev -> ev.getCaseId().equals(c.getCaseId())).forEach(ev -> sb.append("  • [").append(ev.getEvidenceId()).append("] ").append(ev.getType()).append(": ").append(ev.getDescription()).append("\n"));
        showTextAlert("Case File Intelligence", c.getCaseId() + ": " + c.getTitle(), sb.toString());
    }

    /** ================== SUSPECT REGISTRY ================== */
    @SuppressWarnings("unchecked")
    private BorderPane createSuspectPane() {
        BorderPane pane = new BorderPane(); pane.getStyleClass().add(CLASS_CONTENT_PANE);

        TextField searchField = new TextField(); searchField.setPromptText("Search Suspects...");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "Wanted", "In Custody", "Under Surveillance", "Cleared"));
        statusBox.setValue("All Statuses");

        pane.setTop(createFilterBar(searchField, statusBox, () -> { searchField.clear(); statusBox.setValue("All Statuses"); }));

        suspectTable = new TableView<>();
        suspectTable.getColumns().addAll(
                createCol("Suspect ID", 110, Suspect::getId), createCol("Full Name", 200, s -> s.getFirstName() + " " + s.getLastName()),
                createCol("DOB", 110, Suspect::getDateOfBirth), createCol("Traits", 240, Suspect::getPhysicalTraits), createCol("Status", 140, Suspect::getStatus)
        );

        FilteredList<Suspect> filtered = new FilteredList<>(masterSuspectList, p -> true);
        Runnable filterLogic = () -> applyFilter(filtered, searchField.getText(),
                s -> statusBox.getValue().equals("All Statuses") || s.getStatus().equalsIgnoreCase(statusBox.getValue()),
                s -> s.getId() + " " + s.getFirstName() + " " + s.getLastName() + " " + s.getPhysicalTraits());

        searchField.textProperty().addListener(o -> filterLogic.run()); statusBox.valueProperty().addListener(o -> filterLogic.run());
        suspectTable.setItems(bindSort(filtered, suspectTable));

        setupTableInteractions(suspectTable, this::showSuspectDetails,
                createMenuItem("View Dossier", e -> runIfSelected(suspectTable, this::showSuspectDetails)),
                createMenuItem("Edit Profile", e -> runIfSelected(suspectTable, this::openEditSuspectDialog)),
                createMenuItem("Link to Case", e -> openLinkSuspectDialog()),
                createMenuItem("Quick Status", e -> runIfSelected(suspectTable, this::quickUpdateSuspectStatus))
        );
        pane.setCenter(suspectTable);

        pane.setBottom(createActionBar(
                createButton("Register Suspect", e -> openRegisterSuspectDialog()),
                createButton("Edit Selected Profile", e -> requireSelection(suspectTable, this::openEditSuspectDialog)),
                createButton("Link to Case", e -> openLinkSuspectDialog()),
                createButton("Quick Status", e -> requireSelection(suspectTable, this::quickUpdateSuspectStatus)),
                createButton("View Dossier", e -> requireSelection(suspectTable, this::showSuspectDetails))
        ));
        return pane;
    }

    private void openRegisterSuspectDialog() {
        Dialog<ButtonType> dialog = buildDialog("Register Suspect Profile", "Enter suspect background details", "Register");
        TextField fNameField = new TextField(), lNameField = new TextField(), traitsField = new TextField();
        DatePicker dobPicker = new DatePicker(LocalDate.of(1990, 1, 1));
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Wanted", "In Custody", "Under Surveillance", "Cleared"));
        statusBox.setValue("Wanted");

        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("First Name:"), fNameField); grid.addRow(1, new Label("Last Name:"), lNameField);
        grid.addRow(2, new Label("DOB:"), dobPicker); grid.addRow(3, new Label("Traits:"), traitsField); grid.addRow(4, new Label("Status:"), statusBox);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        saveBtn.setDisable(true);
        Runnable val = () -> saveBtn.setDisable(fNameField.getText().trim().isEmpty() || lNameField.getText().trim().isEmpty());
        fNameField.textProperty().addListener(o -> val.run()); lNameField.textProperty().addListener(o -> val.run());

        if (showAndConfirm(dialog)) {
            String id = "S-" + String.format("%04d", db.suspects.size() + 1);
            db.suspects.put(id, new Suspect(id, fNameField.getText().trim(), lNameField.getText().trim(), dobPicker.getValue() != null ? dobPicker.getValue().toString() : "Unknown", traitsField.getText().isEmpty() ? "None recorded" : traitsField.getText(), statusBox.getValue()));
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Success", "Suspect " + id + " registered.");
        }
    }

    private void openEditSuspectDialog(Suspect s) {
        Dialog<ButtonType> dialog = buildDialog("Edit Suspect Profile", "Editing Suspect: " + s.getId(), "Save Changes");
        TextField fNameField = new TextField(s.getFirstName()), lNameField = new TextField(s.getLastName()), traitsField = new TextField(s.getPhysicalTraits());
        DatePicker dobPicker = new DatePicker(); try { dobPicker.setValue(LocalDate.parse(s.getDateOfBirth())); } catch (Exception ignored) {}
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("Wanted", "In Custody", "Under Surveillance", "Cleared"));
        statusBox.setValue(s.getStatus());

        ListView<String> aliasesList = new ListView<>(FXCollections.observableArrayList(s.getAliases())); aliasesList.setPrefHeight(90);
        TextField newAlias = new TextField(); newAlias.setPromptText("Add new alias...");
        Button addAliasBtn = createButton("+ Add", e -> { if(!newAlias.getText().isEmpty() && !aliasesList.getItems().contains(newAlias.getText())) { aliasesList.getItems().add(newAlias.getText()); newAlias.clear(); }});
        Button remAliasBtn = createButton("Remove", e -> { String sel = aliasesList.getSelectionModel().getSelectedItem(); if(sel!=null) aliasesList.getItems().remove(sel);});

        GridPane grid = createFormGrid();
        grid.addRow(0, new Label("Suspect ID:"), new Label(s.getId())); grid.addRow(1, new Label("First Name:"), fNameField);
        grid.addRow(2, new Label("Last Name:"), lNameField); grid.addRow(3, new Label("DOB:"), dobPicker);
        grid.addRow(4, new Label("Traits:"), traitsField); grid.addRow(5, new Label("Status:"), statusBox);
        grid.addRow(6, new Label("Aliases:"), new VBox(6, aliasesList, new HBox(8, newAlias, addAliasBtn, remAliasBtn)));
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        Runnable val = () -> saveBtn.setDisable(fNameField.getText().trim().isEmpty() || lNameField.getText().trim().isEmpty());
        fNameField.textProperty().addListener(o -> val.run()); lNameField.textProperty().addListener(o -> val.run());

        if (showAndConfirm(dialog)) {
            s.setFirstName(fNameField.getText().trim()); s.setLastName(lNameField.getText().trim());
            s.setDateOfBirth(dobPicker.getValue() != null ? dobPicker.getValue().toString() : "Unknown");
            s.setPhysicalTraits(traitsField.getText().trim()); s.setStatus(statusBox.getValue());
            new ArrayList<>(s.getAliases()).forEach(s::removeAlias); aliasesList.getItems().forEach(s::addAlias);
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Updated", "Suspect profile saved.");
        }
    }

    private void quickUpdateSuspectStatus(Suspect s) {
        String status = promptChoice("New status for " + s.getId() + ":", Arrays.asList("Wanted", "In Custody", "Under Surveillance", "Cleared"), s.getStatus());
        if (status != null) { s.setStatus(status); refreshAllDisplays(); }
    }

    private void openLinkSuspectDialog() {
        if (db.suspects.isEmpty() || db.cases.isEmpty()) { showAlert(Alert.AlertType.WARNING, "Cannot Link", "Need at least 1 suspect and 1 case."); return; }
        Dialog<ButtonType> dialog = buildDialog("Link Suspect to Case", "Choose a suspect and case to link", "Link");

        ComboBox<Suspect> sBox = new ComboBox<>(FXCollections.observableArrayList(db.suspects.values()));
        sBox.setConverter(displayConverter(s -> s.getId() + " - " + s.getLastName())); sBox.setValue(suspectTable.getSelectionModel().getSelectedItem());
        ComboBox<Case> cBox = new ComboBox<>(FXCollections.observableArrayList(db.cases.values()));
        cBox.setConverter(displayConverter(c -> c.getCaseId() + " - " + c.getTitle()));

        GridPane grid = createFormGrid(); grid.addRow(0, new Label("Suspect:"), sBox); grid.addRow(1, new Label("Case:"), cBox);
        dialog.getDialogPane().setContent(grid);

        if (showAndConfirm(dialog)) {
            if (sBox.getValue() == null || cBox.getValue() == null) { showAlert(Alert.AlertType.ERROR, "Error", "Select both."); return; }
            if (sBox.getValue().getLinkedCaseIds().contains(cBox.getValue().getCaseId())) { showAlert(Alert.AlertType.WARNING, "Already Linked", "Already linked."); return; }
            sBox.getValue().linkCase(cBox.getValue().getCaseId()); cBox.getValue().addSuspect(sBox.getValue().getId());
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Success", "Linked successfully.");
        }
    }

    private void showSuspectDetails(Suspect s) {
        StringBuilder sb = new StringBuilder("Suspect ID: ").append(s.getId()).append("\nName: ").append(s.getFirstName()).append(" ").append(s.getLastName())
                .append("\nDOB: ").append(s.getDateOfBirth()).append("\nStatus: ").append(s.getStatus()).append("\nTraits: ").append(s.getPhysicalTraits())
                .append("\n\n--- ALIASES ---\n");
        s.getAliases().forEach(a -> sb.append("  • ").append(a).append("\n"));
        if(s.getAliases().isEmpty()) sb.append("  (None)\n");
        sb.append("\n--- LINKED CASES ---\n");
        s.getLinkedCaseIds().forEach(cid -> { Case c = db.cases.get(cid); sb.append("  • [").append(cid).append("] ").append(c != null ? c.getTitle() : "Unknown").append("\n"); });
        showTextAlert("Suspect Dossier", s.getId() + ": " + s.getLastName(), sb.toString());
    }

    /** ================== EVIDENCE LOCKER ================== */
    @SuppressWarnings("unchecked")
    private BorderPane createEvidencePane() {
        BorderPane pane = new BorderPane(); pane.getStyleClass().add(CLASS_CONTENT_PANE);

        TextField searchField = new TextField(); searchField.setPromptText("Search Evidence...");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("All Types", "Weapon", "Digital", "Document", "Biological")); typeBox.setValue("All Types");
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("All Statuses", "In Storage", "At Lab/Forensics", "Released/Destroyed")); statusBox.setValue("All Statuses");

        HBox filterBar = createFilterBar(searchField, typeBox, () -> { searchField.clear(); typeBox.setValue("All Types"); statusBox.setValue("All Statuses"); });
        filterBar.getChildren().add(2, new Label("Status:")); filterBar.getChildren().add(3, statusBox);
        pane.setTop(filterBar);

        evidenceTable = new TableView<>();
        evidenceTable.getColumns().addAll(
                createCol("ID", 110, Evidence::getEvidenceId), createCol("Case ID", 110, Evidence::getCaseId),
                createCol("Category", 120, Evidence::getType), createCol("Description", 280, Evidence::getDescription), createCol("Status", 150, Evidence::getStatus)
        );

        FilteredList<Evidence> filtered = new FilteredList<>(masterEvidenceList, p -> true);
        Runnable filterLogic = () -> applyFilter(filtered, searchField.getText(),
                ev -> (typeBox.getValue().equals("All Types") || ev.getType().equalsIgnoreCase(typeBox.getValue())) &&
                        (statusBox.getValue().equals("All Statuses") || ev.getStatus().equalsIgnoreCase(statusBox.getValue())),
                ev -> ev.getEvidenceId() + " " + ev.getCaseId() + " " + ev.getDescription());

        searchField.textProperty().addListener(o -> filterLogic.run()); typeBox.valueProperty().addListener(o -> filterLogic.run()); statusBox.valueProperty().addListener(o -> filterLogic.run());
        evidenceTable.setItems(bindSort(filtered, evidenceTable));

        setupTableInteractions(evidenceTable, this::showCustodyLog,
                createMenuItem("View Custody Log", e -> runIfSelected(evidenceTable, this::showCustodyLog)),
                createMenuItem("Edit Record", e -> runIfSelected(evidenceTable, this::openEditEvidenceDialog)),
                createMenuItem("Update Status", e -> runIfSelected(evidenceTable, this::quickUpdateEvidenceStatus))
        );
        pane.setCenter(evidenceTable);

        pane.setBottom(createActionBar(
                createButton("Log New Evidence", e -> openLogEvidenceDialog()),
                createButton("Edit Selected Item", e -> requireSelection(evidenceTable, this::openEditEvidenceDialog)),
                createButton("Update Custody", e -> requireSelection(evidenceTable, this::quickUpdateEvidenceStatus)),
                createButton("View Custody History", e -> requireSelection(evidenceTable, this::showCustodyLog))
        ));
        return pane;
    }

    private void openLogEvidenceDialog() {
        if (db.cases.isEmpty()) { showAlert(Alert.AlertType.WARNING, "No Cases", "Create a case first."); return; }
        Dialog<ButtonType> dialog = buildDialog("Log Evidence", "Link to investigation", "Log Item");

        ComboBox<Case> cBox = new ComboBox<>(FXCollections.observableArrayList(db.cases.values()));
        cBox.setConverter(displayConverter(c -> c.getCaseId() + " - " + c.getTitle())); cBox.setValue(caseTable != null ? caseTable.getSelectionModel().getSelectedItem() : null);
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Weapon", "Digital", "Document", "Biological")); typeBox.setValue("Weapon");
        TextField descField = new TextField();

        GridPane grid = createFormGrid(); grid.addRow(0, new Label("Case:"), cBox); grid.addRow(1, new Label("Type:"), typeBox); grid.addRow(2, new Label("Description:"), descField);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst()); saveBtn.setDisable(true);
        Runnable val = () -> saveBtn.setDisable(cBox.getValue() == null || descField.getText().trim().isEmpty());
        cBox.valueProperty().addListener(o -> val.run()); descField.textProperty().addListener(o -> val.run());

        if (showAndConfirm(dialog)) {
            String id = "EV-" + String.format("%04d", db.evidence.size() + 1);
            Evidence ev = new Evidence(id, cBox.getValue().getCaseId(), typeBox.getValue(), descField.getText().trim(), "In Storage");
            ev.logCustody("Initial intake logged by Agent " + loggedInUser.getLastName() + " at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            db.evidence.put(id, ev); cBox.getValue().addEvidence(id);
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Logged", "Evidence " + id + " logged.");
        }
    }

    private void openEditEvidenceDialog(Evidence ev) {
        Dialog<ButtonType> dialog = buildDialog("Edit Evidence Record", "Editing Evidence: " + ev.getEvidenceId(), "Save Changes");
        ComboBox<Case> cBox = new ComboBox<>(FXCollections.observableArrayList(db.cases.values()));
        cBox.setConverter(displayConverter(c -> c.getCaseId() + " - " + c.getTitle())); cBox.setValue(db.cases.get(ev.getCaseId()));
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Weapon", "Digital", "Document", "Biological")); typeBox.setValue(ev.getType());
        ComboBox<String> statusBox = new ComboBox<>(FXCollections.observableArrayList("In Storage", "At Lab/Forensics", "Released/Destroyed")); statusBox.setValue(ev.getStatus());
        TextField descField = new TextField(ev.getDescription());

        GridPane grid = createFormGrid(); grid.addRow(0, new Label("ID:"), new Label(ev.getEvidenceId()));
        grid.addRow(1, new Label("Case:"), cBox); grid.addRow(2, new Label("Type:"), typeBox);
        grid.addRow(3, new Label("Description:"), descField); grid.addRow(4, new Label("Status:"), statusBox);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        descField.textProperty().addListener((o, old, newVal) -> saveBtn.setDisable(newVal.trim().isEmpty() || cBox.getValue() == null));


        if (showAndConfirm(dialog)) {
            if (cBox.getValue() != null && !cBox.getValue().getCaseId().equals(ev.getCaseId())) { ev.setCaseId(cBox.getValue().getCaseId()); cBox.getValue().addEvidence(ev.getEvidenceId()); }
            String oldStat = ev.getStatus(); ev.setType(typeBox.getValue()); ev.setDescription(descField.getText().trim()); ev.setStatus(statusBox.getValue());
            ev.logCustody("Edited (Status: " + oldStat + " -> " + ev.getStatus() + ") by " + loggedInUser.getLastName() + " at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Updated", "Record updated.");
        }
    }

    private void quickUpdateEvidenceStatus(Evidence ev) {
        String status = promptChoice("New Status for " + ev.getEvidenceId() + ":", Arrays.asList("In Storage", "At Lab/Forensics", "Released/Destroyed"), ev.getStatus());
        if (status != null) { ev.setStatus(status); ev.logCustody("Status -> " + status + " by " + loggedInUser.getLastName() + " at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))); refreshAllDisplays(); }
    }

    private void showCustodyLog(Evidence ev) {
        StringBuilder sb = new StringBuilder("Evidence ID: ").append(ev.getEvidenceId()).append("\nCategory: ").append(ev.getType()).append("\nCase: ").append(ev.getCaseId()).append("\nDesc: ").append(ev.getDescription()).append("\n\n--- CUSTODY LOG ---\n");
        ev.getCustodyLog().forEach(l -> sb.append("• ").append(l).append("\n"));
        if (ev.getCustodyLog().isEmpty()) sb.append("No logs recorded.\n");
        showTextAlert("Chain of Custody Report", "Evidence: " + ev.getEvidenceId(), sb.toString());
    }


    /** ================== ADMIN CONSOLE ================== */
    @SuppressWarnings("unchecked")
    private VBox createAdminPane() {
        VBox pane = new VBox(14); pane.setAlignment(Pos.CENTER); pane.getStyleClass().add(CLASS_CONTENT_PANE); pane.setPadding(new Insets(18));

        TextField aSearch = new TextField(); aSearch.setPromptText("Filter agents...");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("All Roles", "Admin", "Field Agent")); roleBox.setValue("All Roles");
        HBox filterBar = new HBox(10, new Label("Agents:"), aSearch, new Label("Role:"), roleBox); filterBar.setAlignment(Pos.CENTER_LEFT);

        userTable = new TableView<>(); userTable.setPrefHeight(160);
        userTable.getColumns().addAll(createCol("ID", 100, User::getId), createCol("Username", 140, User::getUsername), createCol("Last Name", 160, User::getLastName), createCol("Role", 140, User::getRole));

        FilteredList<User> filtered = new FilteredList<>(masterUserList, p -> true);
        Runnable filterLogic = () -> applyFilter(filtered, aSearch.getText(), u -> roleBox.getValue().equals("All Roles") || u.getRole().equalsIgnoreCase(roleBox.getValue()), u -> u.getId() + " " + u.getUsername() + " " + u.getLastName());
        aSearch.textProperty().addListener(o -> filterLogic.run()); roleBox.valueProperty().addListener(o -> filterLogic.run());
        userTable.setItems(bindSort(filtered, userTable));

        deleteTypeChoice = new ComboBox<>(FXCollections.observableArrayList("Case", "Suspect", "Evidence")); deleteTypeChoice.setValue("Case"); deleteTypeChoice.setOnAction(e -> refreshDeleteList());
        deleteListView = new ListView<>(); deleteListView.setPrefHeight(120);

        Button delBtn = createButton("Delete Selected Record", e -> {
            String sel = deleteListView.getSelectionModel().getSelectedItem(); if (sel == null) return;
            String id = sel.split("\\|")[0].trim();
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Irreversible purge. Continue?", ButtonType.OK, ButtonType.CANCEL);
            confirm.setTitle("Confirm Purge"); applyTheme(confirm.getDialogPane());
            if (confirm.showAndWait().orElse(null) == ButtonType.OK) {
                if(db.cases.remove(id) != null || db.suspects.remove(id) != null || db.evidence.remove(id) != null) { refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Purged", "Record deleted."); }
            }
        }); delBtn.getStyleClass().add(CLASS_DANGER_BUTTON);

        pane.getChildren().addAll(
                new Label("ADMINISTRATOR CONSOLE") {{ getStyleClass().add(CLASS_DANGER_TEXT); setFont(Font.font("Arial", FontWeight.BOLD, 18)); }},
                filterBar, userTable, createActionBar(createButton("Register Agent", e -> openRegisterAgentDialog()), createButton("Edit Agent", e -> requireSelection(userTable, this::openEditAgentDialog))),
                new Separator(), new VBox(8, new Label("Purge Classified Record:"), new HBox(10, new Label("Category:"), deleteTypeChoice), deleteListView, delBtn)
        );
        return pane;
    }

    private void openRegisterAgentDialog() {
        Dialog<ButtonType> dialog = buildDialog("Register New Personnel", "Create Agent Account", "Register");
        TextField uField = new TextField(), lField = new TextField(); PasswordField pField = new PasswordField();
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Field Agent", "Admin")); roleBox.setValue("Field Agent");

        GridPane grid = createFormGrid(); grid.addRow(0, new Label("Username:"), uField); grid.addRow(1, new Label("Password:"), pField);
        grid.addRow(2, new Label("Last Name:"), lField); grid.addRow(3, new Label("Role:"), roleBox); dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst()); saveBtn.setDisable(true);
        Runnable val = () -> saveBtn.setDisable(uField.getText().trim().isEmpty() || pField.getText().isEmpty() || lField.getText().trim().isEmpty());
        uField.textProperty().addListener(o -> val.run()); pField.textProperty().addListener(o -> val.run()); lField.textProperty().addListener(o -> val.run());

        if (showAndConfirm(dialog)) {
            String id = "U-" + String.format("%03d", db.users.size() + 1);
            db.users.put(id, new User(id, "Agent", lField.getText().trim(), "N/A", uField.getText().trim(), pField.getText(), roleBox.getValue()));
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Success", "Agent added.");
        }
    }

    private void openEditAgentDialog(User u) {
        Dialog<ButtonType> dialog = buildDialog("Edit Agent", "Editing: " + u.getId(), "Save Changes");
        TextField uField = new TextField(u.getUsername()), lField = new TextField(u.getLastName()); PasswordField pField = new PasswordField(); pField.setPromptText("Leave empty to keep current");
        ComboBox<String> roleBox = new ComboBox<>(FXCollections.observableArrayList("Field Agent", "Admin")); roleBox.setValue(u.getRole());

        GridPane grid = createFormGrid(); grid.addRow(0, new Label("ID:"), new Label(u.getId())); grid.addRow(1, new Label("Username:"), uField);
        grid.addRow(2, new Label("Last Name:"), lField); grid.addRow(3, new Label("Role:"), roleBox); grid.addRow(4, new Label("Reset PW:"), pField);
        dialog.getDialogPane().setContent(grid);

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().getFirst());
        Runnable val = () -> saveBtn.setDisable(uField.getText().trim().isEmpty() || lField.getText().trim().isEmpty());
        uField.textProperty().addListener(o -> val.run()); lField.textProperty().addListener(o -> val.run());

        if (showAndConfirm(dialog)) {
            u.setUsername(uField.getText().trim()); u.setLastName(lField.getText().trim()); u.setRole(roleBox.getValue());
            if (!pField.getText().isEmpty()) u.setPassword(pField.getText()); refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Updated", "Agent profile updated.");
        }
    }

    private void refreshDeleteList() {
        if (deleteTypeChoice == null || deleteListView == null) return;
        ObservableList<String> items = FXCollections.observableArrayList(); String type = deleteTypeChoice.getValue();
        if ("Case".equals(type)) db.cases.values().forEach(c -> items.add(c.getCaseId() + " | " + c.getTitle() + " (" + c.getStatus() + ")"));
        else if ("Suspect".equals(type)) db.suspects.values().forEach(s -> items.add(s.getId() + " | " + s.getLastName() + " (" + s.getStatus() + ")"));
        else if ("Evidence".equals(type)) db.evidence.values().forEach(ev -> items.add(ev.getEvidenceId() + " | " + ev.getType() + " (Case " + ev.getCaseId() + ")"));
        deleteListView.setItems(items);
    }

    /** ================== UTILITIES & HELPERS ================== */
    private void refreshAllDisplays() {
        masterCaseList.setAll(db.cases.values()); masterSuspectList.setAll(db.suspects.values()); updateHomeStats();
        masterEvidenceList.setAll(db.evidence.values()); masterUserList.setAll(db.users.values()); refreshDeleteList();
    }
    private void validateAgentCredentials(String username, String password) throws RegistryValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new RegistryValidationException("Username cannot be blank.");
        }
        if (password == null || password.length() < 4) {
            throw new RegistryValidationException("Security password must be at least 4 characters.");
        }
    }
    // Prevents NullPointerExceptions when the user cancels or closes the dialog.

    private boolean showAndConfirm(Dialog<ButtonType> dialog) {
        return dialog.showAndWait()
                .filter(b -> b.getButtonData().isDefaultButton())
                .isPresent();
    }
    //S=Source, T=Target.

    private <S> TableColumn<S, String> createCol(String title, double width, Function<S, String> mapper) {
        TableColumn<S, String> col = new TableColumn<>(title);
        col.setCellValueFactory(data -> new SimpleStringProperty(mapper.apply(data.getValue())));
        col.setPrefWidth(width);
        return col;
    }


    private <T> void setupTableInteractions(TableView<T> table, Consumer<T> onDoubleClick, MenuItem... items) {
        ContextMenu ctx = new ContextMenu(items);
        table.setRowFactory(tv -> { TableRow<T> row = new TableRow<>(); row.setOnMouseClicked(e -> { if(e.getClickCount() == 2 && !row.isEmpty()) onDoubleClick.accept(row.getItem()); }); row.setContextMenu(ctx); return row; });
    }

    private <T> void runIfSelected(TableView<T> table, Consumer<T> action) { T sel = table.getSelectionModel().getSelectedItem(); if (sel != null) action.accept(sel); }
    private <T> void requireSelection(TableView<T> table, Consumer<T> action) { T sel = table.getSelectionModel().getSelectedItem(); if (sel == null) showAlert(Alert.AlertType.WARNING, "No Selection", "Please select an item from the table first."); else action.accept(sel); }

    private <T> void applyFilter(FilteredList<T> list, String query, Function<T, Boolean> statusMatch, Function<T, String> textMatch) {
        String q = query.toLowerCase().trim(); list.setPredicate(item -> statusMatch.apply(item) && (q.isEmpty() || textMatch.apply(item).toLowerCase().contains(q)));
    }

    private <T> SortedList<T> bindSort(FilteredList<T> filtered, TableView<T> table) { SortedList<T> sorted = new SortedList<>(filtered); sorted.comparatorProperty().bind(table.comparatorProperty()); return sorted; }

    private HBox createFilterBar(Node... nodes) { HBox box = new HBox(12, nodes); box.setAlignment(Pos.CENTER_LEFT); box.setPadding(new Insets(10)); box.getStyleClass().add(CLASS_FILTER_BAR); return box; }
    private HBox createFilterBar(TextField search, ComboBox<String> cb, Runnable onClear) { return createFilterBar(new Label("Search:"), search, new Label("Filter:"), cb, createButton("✕ Clear", e -> onClear.run())); }
    private HBox createActionBar(Node... nodes) { HBox box = new HBox(12, nodes); box.setPadding(new Insets(10)); box.setAlignment(Pos.CENTER); box.getStyleClass().add(CLASS_ACTION_BAR); return box; }

    private Button createButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) { Button b = new Button(text); b.setOnAction(action); return b; }
    private MenuItem createMenuItem(String text, javafx.event.EventHandler<javafx.event.ActionEvent> action) { MenuItem m = new MenuItem(text); m.setOnAction(action); return m; }

    private GridPane createFormGrid() { GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20)); return grid; }

    private Dialog<ButtonType> buildDialog(String title, String header, String okText) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(title); dialog.setHeaderText(header);
        dialog.getDialogPane().getButtonTypes().addAll(new ButtonType(okText, ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL); applyTheme(dialog.getDialogPane()); return dialog;
    }

    private String promptText() { TextInputDialog dialog = new TextInputDialog(); dialog.setTitle("Export Dossier"); dialog.setHeaderText("Enter Case ID:"); applyTheme(dialog.getDialogPane()); return dialog.showAndWait().orElse(null); }
    private String promptChoice(String header, List<String> choices, String defaultChoice) { ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices); dialog.setTitle("Update Status"); dialog.setHeaderText(header); applyTheme(dialog.getDialogPane()); return dialog.showAndWait().orElse(null); }

    private void showAlert(Alert.AlertType type, String title, String content) { Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); applyTheme(alert.getDialogPane()); alert.showAndWait(); }
    private void showTextAlert(String title, String header, String text) { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(header); TextArea area = new TextArea(text); area.setEditable(false); area.setWrapText(true); area.setPrefSize(480, 340); alert.getDialogPane().setContent(area); applyTheme(alert.getDialogPane()); alert.showAndWait(); }

    private void applyTheme(DialogPane pane) {
        if (stylesheetUrl != null && pane != null) {
            pane.getStylesheets().add(stylesheetUrl);
        }
    }
    private String getStylesheetUrl() { URL url = FederalRegistryFX.class.getResource("style.css"); return url != null ? url.toExternalForm() : null; }

    private <T> StringConverter<T> displayConverter(Function<T, String> displayFn) { return new StringConverter<>() { @Override public String toString(T obj) { return obj == null ? "" : displayFn.apply(obj); } @Override public T fromString(String string) { return null; } }; }

    private void generateDossier(String caseId) {
        Case c = db.cases.get(caseId); if (c == null) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("Dossier_" + caseId + ".txt"))) {
            bw.write("=== OFFICIAL CASE DOSSIER ===\nCase ID: " + c.getCaseId() + "\nTitle: " + c.getTitle() + "\nStatus: " + c.getStatus() + "\n");
        } catch (IOException e) { System.err.println("File operation error: " + e.getMessage()); }
    }

    public static void main(String[] args) { launch(args); }
}