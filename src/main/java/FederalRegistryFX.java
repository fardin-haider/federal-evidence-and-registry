import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.value.ObservableValue;
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
import java.util.function.Consumer;
import java.util.function.Function;

public class FederalRegistryFX extends Application {
    private DataManager db = new DataManager();
    private User loggedInUser = null;

    private Stage primaryStage;
    private Scene loginScene, dashboardScene;
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
    private Tab adminTab;
    private TextArea searchDisplay;
    private ComboBox<String> deleteTypeChoice;
    private ListView<String> deleteListView;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        db.loadData();
        if (db.users.isEmpty()) db.users.put("U-001", new User("U-001", "System", "Admin", "1980-01-01", "admin", "admin123", "Admin"));
        stylesheetUrl = getStylesheetUrl();

        primaryStage.setTitle("Federal Evidence & Suspect Registry");
        primaryStage.setOnCloseRequest(e -> { db.saveData(); System.exit(0); });

        loginScene = new Scene(createLoginPane(), 920, 680);
        dashboardScene = new Scene(createDashboardPane(), 960, 700);

        applyTheme(loginScene.getRoot());
        applyTheme(dashboardScene.getRoot());

        primaryStage.setScene(loginScene);
        primaryStage.show();
    }

    /** ================== LOGIN ================== */
    private GridPane createLoginPane() {
        GridPane grid = createFormGrid();
        grid.setAlignment(Pos.CENTER);

        Label title = new Label("FEDERAL REGISTRY AUTHENTICATION");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 22));
        title.getStyleClass().add(CLASS_LOGIN_TITLE);

        TextField userField = new TextField(); userField.setPromptText("Enter agent username");
        PasswordField pwBox = new PasswordField(); pwBox.setPromptText("Enter secure password");
        Label errorLabel = new Label(); errorLabel.getStyleClass().add(CLASS_DANGER_TEXT);

        Button btn = new Button("Authenticate"); btn.setDefaultButton(true);
        btn.setOnAction(e -> authenticate(userField, pwBox, errorLabel));

        grid.add(title, 0, 0, 2, 1);
        grid.addRow(1, new Label("Username:"), userField);
        grid.addRow(2, new Label("Password:"), pwBox);
        grid.add(errorLabel, 0, 3, 2, 1);
        grid.add(new HBox(btn) {{ setAlignment(Pos.BOTTOM_RIGHT); }}, 1, 4);

        return grid;
    }

    private void authenticate(TextField userField, PasswordField pwBox, Label errorLabel) {
        for (User u : db.users.values()) {
            if (u.getUsername().equalsIgnoreCase(userField.getText().trim()) && u.getPassword().equals(pwBox.getText())) {
                loggedInUser = u;
                welcomeLabel.setText("Active Agent: " + u.getLastName() + " | Role: " + u.getRole());
                adminTab.setDisable(!u.getRole().equalsIgnoreCase("Admin"));
                refreshAllDisplays();
                primaryStage.setScene(dashboardScene);
                userField.clear(); pwBox.clear(); errorLabel.setText("");
                return;
            }
        }
        errorLabel.setText("Access Denied: Invalid agent credentials.");
        showAlert(Alert.AlertType.ERROR, "Authentication Error", "Access Denied. Invalid credentials.");
    }

    /** ================== DASHBOARD & TABS ================== */
    private BorderPane createDashboardPane() {
        BorderPane borderPane = new BorderPane();
        BorderPane header = new BorderPane();
        header.getStyleClass().add(CLASS_HEADER_BAR);

        welcomeLabel = new Label(); welcomeLabel.getStyleClass().add(CLASS_WELCOME_LABEL);
        Button logoutBtn = new Button("Logout"); logoutBtn.getStyleClass().add(CLASS_DANGER_BUTTON);
        logoutBtn.setOnAction(e -> { loggedInUser = null; primaryStage.setScene(loginScene); });

        header.setLeft(welcomeLabel); header.setRight(logoutBtn);
        borderPane.setTop(header);

        TabPane tabPane = new TabPane(); tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
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

    /** ================== HOME PAGE ================== */
    private BorderPane createHomePane() {
        BorderPane homePane = new BorderPane();
        homePane.setPadding(new Insets(15));

        String userInfo;
        if (loggedInUser != null) {
            userInfo = "Welcome, " + loggedInUser.getLastName() + " (" + loggedInUser.getRole() + ")";
        } else {
            userInfo = "Welcome to the Federal Registry System";
        }

        Label welcome = new Label(userInfo);
        welcome.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        homePane.setTop(welcome);
        BorderPane.setAlignment(welcome, Pos.CENTER);

        HBox statsBox = new HBox(20);
        statsBox.setAlignment(Pos.CENTER);
        Label caseCount = new Label("Cases: " + db.cases.size());
        Label suspectCount = new Label("Suspects: " + db.suspects.size());
        Label evidenceCount = new Label("Evidence: " + db.evidence.size());
        statsBox.getChildren().addAll(caseCount, suspectCount, evidenceCount);
        homePane.setCenter(statsBox);
        return homePane;
    }

    /** ================== CASE MANAGEMENT ================== */
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
                createButton("Export Dossier", e -> {
                    String id = promptText("Export Dossier", "Enter Case ID:");
                    if (id != null && db.cases.containsKey(id.trim())) { generateDossier(id.trim()); showAlert(Alert.AlertType.INFORMATION, "Exported", "Dossier exported to Dossier_" + id.trim() + ".txt"); }
                    else if (id != null) showAlert(Alert.AlertType.ERROR, "Not Found", "Case ID not found.");
                })
        ));
        return pane;
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

        Node createBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        createBtn.setDisable(true); titleField.textProperty().addListener((o, old, newVal) -> createBtn.setDisable(newVal.trim().isEmpty()));

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        titleField.textProperty().addListener((o, old, newVal) -> saveBtn.setDisable(newVal.trim().isEmpty()));

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
            c.setTitle(titleField.getText().trim()); c.setStatus(statusBox.getValue());
            refreshAllDisplays();
            showAlert(Alert.AlertType.INFORMATION, "Case Updated", "Changes saved successfully.");
        }
    }

    private void quickUpdateCaseStatus(Case c) {
        String status = promptChoice("Update Status", "Select new status for " + c.getCaseId() + ":", Arrays.asList("Open", "Closed", "Cold"), c.getStatus());
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        saveBtn.setDisable(true);
        Runnable val = () -> saveBtn.setDisable(fNameField.getText().trim().isEmpty() || lNameField.getText().trim().isEmpty());
        fNameField.textProperty().addListener(o -> val.run()); lNameField.textProperty().addListener(o -> val.run());

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        Runnable val = () -> saveBtn.setDisable(fNameField.getText().trim().isEmpty() || lNameField.getText().trim().isEmpty());
        fNameField.textProperty().addListener(o -> val.run()); lNameField.textProperty().addListener(o -> val.run());

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
            s.setFirstName(fNameField.getText().trim()); s.setLastName(lNameField.getText().trim());
            s.setDateOfBirth(dobPicker.getValue() != null ? dobPicker.getValue().toString() : "Unknown");
            s.setPhysicalTraits(traitsField.getText().trim()); s.setStatus(statusBox.getValue());
            new ArrayList<>(s.getAliases()).forEach(s::removeAlias); aliasesList.getItems().forEach(s::addAlias);
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Updated", "Suspect profile saved.");
        }
    }

    private void quickUpdateSuspectStatus(Suspect s) {
        String status = promptChoice("Update Status", "New status for " + s.getId() + ":", Arrays.asList("Wanted", "In Custody", "Under Surveillance", "Cleared"), s.getStatus());
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

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0)); saveBtn.setDisable(true);
        Runnable val = () -> saveBtn.setDisable(cBox.getValue() == null || descField.getText().trim().isEmpty());
        cBox.valueProperty().addListener(o -> val.run()); descField.textProperty().addListener(o -> val.run());

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        descField.textProperty().addListener((o, old, newVal) -> saveBtn.setDisable(newVal.trim().isEmpty() || cBox.getValue() == null));

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
            if (cBox.getValue() != null && !cBox.getValue().getCaseId().equals(ev.getCaseId())) { ev.setCaseId(cBox.getValue().getCaseId()); cBox.getValue().addEvidence(ev.getEvidenceId()); }
            String oldStat = ev.getStatus(); ev.setType(typeBox.getValue()); ev.setDescription(descField.getText().trim()); ev.setStatus(statusBox.getValue());
            ev.logCustody("Edited (Status: " + oldStat + " -> " + ev.getStatus() + ") by " + loggedInUser.getLastName() + " at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            refreshAllDisplays(); showAlert(Alert.AlertType.INFORMATION, "Updated", "Record updated.");
        }
    }

    private void quickUpdateEvidenceStatus(Evidence ev) {
        String status = promptChoice("Update Status", "New Status for " + ev.getEvidenceId() + ":", Arrays.asList("In Storage", "At Lab/Forensics", "Released/Destroyed"), ev.getStatus());
        if (status != null) { ev.setStatus(status); ev.logCustody("Status -> " + status + " by " + loggedInUser.getLastName() + " at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))); refreshAllDisplays(); }
    }

    private void showCustodyLog(Evidence ev) {
        StringBuilder sb = new StringBuilder("Evidence ID: ").append(ev.getEvidenceId()).append("\nCategory: ").append(ev.getType()).append("\nCase: ").append(ev.getCaseId()).append("\nDesc: ").append(ev.getDescription()).append("\n\n--- CUSTODY LOG ---\n");
        ev.getCustodyLog().forEach(l -> sb.append("• ").append(l).append("\n"));
        if (ev.getCustodyLog().isEmpty()) sb.append("No logs recorded.\n");
        showTextAlert("Chain of Custody Report", "Evidence: " + ev.getEvidenceId(), sb.toString());
    }


    /** ================== ADMIN CONSOLE ================== */
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0)); saveBtn.setDisable(true);
        Runnable val = () -> saveBtn.setDisable(uField.getText().trim().isEmpty() || pField.getText().isEmpty() || lField.getText().trim().isEmpty());
        uField.textProperty().addListener(o -> val.run()); pField.textProperty().addListener(o -> val.run()); lField.textProperty().addListener(o -> val.run());

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
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

        Node saveBtn = dialog.getDialogPane().lookupButton(dialog.getDialogPane().getButtonTypes().get(0));
        Runnable val = () -> saveBtn.setDisable(uField.getText().trim().isEmpty() || lField.getText().trim().isEmpty());
        uField.textProperty().addListener(o -> val.run()); lField.textProperty().addListener(o -> val.run());

        if (dialog.showAndWait().orElse(null).getButtonData().isDefaultButton()) {
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
        masterCaseList.setAll(db.cases.values()); masterSuspectList.setAll(db.suspects.values());
        masterEvidenceList.setAll(db.evidence.values()); masterUserList.setAll(db.users.values()); refreshDeleteList();
    }

    private <S, T> TableColumn<S, T> createCol(String title, double width, Function<S, String> mapper) {
        TableColumn<S, T> col = new TableColumn<>(title); col.setCellValueFactory(data -> (ObservableValue<T>) new SimpleStringProperty(mapper.apply(data.getValue()))); col.setPrefWidth(width); return col;
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

    private String promptText(String title, String header) { TextInputDialog dialog = new TextInputDialog(); dialog.setTitle(title); dialog.setHeaderText(header); applyTheme(dialog.getDialogPane()); return dialog.showAndWait().orElse(null); }
    private String promptChoice(String title, String header, List<String> choices, String defaultChoice) { ChoiceDialog<String> dialog = new ChoiceDialog<>(defaultChoice, choices); dialog.setTitle(title); dialog.setHeaderText(header); applyTheme(dialog.getDialogPane()); return dialog.showAndWait().orElse(null); }

    private void showAlert(Alert.AlertType type, String title, String content) { Alert alert = new Alert(type); alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content); applyTheme(alert.getDialogPane()); alert.showAndWait(); }
    private void showTextAlert(String title, String header, String text) { Alert alert = new Alert(Alert.AlertType.INFORMATION); alert.setTitle(title); alert.setHeaderText(header); TextArea area = new TextArea(text); area.setEditable(false); area.setWrapText(true); area.setPrefSize(480, 340); alert.getDialogPane().setContent(area); applyTheme(alert.getDialogPane()); alert.showAndWait(); }

    private void applyTheme(Node node) { if (stylesheetUrl != null && node instanceof javafx.scene.Parent) ((javafx.scene.Parent)node).getStylesheets().add(stylesheetUrl); else if (stylesheetUrl != null && node instanceof DialogPane) ((DialogPane)node).getStylesheets().add(stylesheetUrl); }
    private String getStylesheetUrl() { URL url = FederalRegistryFX.class.getResource("style.css"); return url != null ? url.toExternalForm() : null; }

    private <T> StringConverter<T> displayConverter(Function<T, String> displayFn) { return new StringConverter<T>() { @Override public String toString(T obj) { return obj == null ? "" : displayFn.apply(obj); } @Override public T fromString(String string) { return null; } }; }

    private void generateDossier(String caseId) {
        Case c = db.cases.get(caseId); if (c == null) return;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("Dossier_" + caseId + ".txt"))) {
            bw.write("=== OFFICIAL CASE DOSSIER ===\nCase ID: " + c.getCaseId() + "\nTitle: " + c.getTitle() + "\nStatus: " + c.getStatus() + "\n");
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void main(String[] args) { launch(args); }
}