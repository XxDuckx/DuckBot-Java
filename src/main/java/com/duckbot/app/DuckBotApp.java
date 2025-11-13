package com.duckbot.app;

import com.duckbot.core.Config;
import com.duckbot.core.RunStatus;
import com.duckbot.services.*;
import com.duckbot.services.impl.*;
import com.duckbot.store.JsonStore;
import com.duckbot.theme.ThemeManager;
import com.duckbot.util.DataPaths;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Entry point for DuckBot JavaFX application.
 */
public final class DuckBotApp extends Application {

    private AuthService authService;
    private ConfigService configService;
    private LogService logService;
    private Config config;

    @Override
    public void start(Stage stage) {
        DataPaths.root();
        JsonStore store = new JsonStore();
        configService = new FileConfigService(store);
        config = configService.load();
        logService = new FileLogService();

        AuthProvider provider = createAuthProvider(store, config);
        authService = new AuthService(provider);
        if (provider instanceof LocalAuthProvider localProvider && localProvider.requiresAdminSetup()) {
            promptAdminCreation(localProvider);
        }

        showLogin(stage);
    }

    private AuthProvider createAuthProvider(JsonStore store, Config config) {
        if ("cloud".equalsIgnoreCase(config.authMode)) {
            return new CloudAuthProvider();
        }
        return new LocalAuthProvider(store);
    }

    private void promptAdminCreation(LocalAuthProvider provider) {
        TextInputDialog usernameDialog = new TextInputDialog("admin");
        usernameDialog.setTitle("Create Admin User");
        usernameDialog.setHeaderText("No users found. Create the initial admin account.");
        usernameDialog.setContentText("Admin username:");
        usernameDialog.showAndWait().ifPresent(username -> {
            Dialog<String> passwordDialog = createPasswordDialog();
            passwordDialog.showAndWait().ifPresent(password -> {
                try {
                    provider.register(username, password);
                } catch (AuthException e) {
                    showError("Unable to create admin: " + e.getMessage());
                }
            });
        });
    }

    private Dialog<String> createPasswordDialog() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Admin Password");
        dialog.setHeaderText("Choose a password for the admin user.");
        ButtonType okButton = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);
        PasswordField passwordField = new PasswordField();
        PasswordField confirmField = new PasswordField();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.addRow(0, new Label("Password:"), passwordField);
        grid.addRow(1, new Label("Confirm:"), confirmField);
        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> {
            if (button == okButton) {
                if (!passwordField.getText().equals(confirmField.getText())) {
                    showError("Passwords do not match.");
                    return null;
                }
                return passwordField.getText();
            }
            return null;
        });
        return dialog;
    }

    private void showLogin(Stage stage) {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-error");

        grid.addRow(0, new Label("Username"), usernameField);
        grid.addRow(1, new Label("Password"), passwordField);
        Button loginButton = new Button("Login");
        grid.addRow(2, loginButton, statusLabel);

        loginButton.setOnAction(event -> {
            try {
                boolean success = authService.login(usernameField.getText(), passwordField.getText());
                if (success) {
                    showMainWindow(stage);
                } else {
                    statusLabel.setText("Login failed");
                }
            } catch (AuthException ex) {
                statusLabel.setText(ex.getMessage());
            }
        });

        Scene scene = new Scene(grid, 400, 200);
        ThemeManager.apply(scene, config.theme);
        stage.setTitle("DuckBot Login");
        stage.setScene(scene);
        stage.show();
    }

    private void showMainWindow(Stage stage) {
        TabPane tabs = new TabPane();
        tabs.getTabs().add(createBotsTab());
        tabs.getTabs().add(createScriptBuilderTab());
        tabs.getTabs().add(createLiveRunnerTab());
        tabs.getTabs().add(createLogsTab());
        tabs.getTabs().add(createSettingsTab());
        tabs.getTabs().add(createUpdatesTab());

        BorderPane root = new BorderPane(tabs);
        Scene scene = new Scene(root, 1000, 700);
        ThemeManager.apply(scene, config.theme);
        stage.setTitle("DuckBot Control Center");
        stage.setScene(scene);
        stage.show();
    }

    private Tab createBotsTab() {
        ListView<String> botsList = new ListView<>(FXCollections.observableArrayList("Example Bot"));
        botsList.setPrefWidth(200);
        VBox editor = new VBox(10, new Label("Select a bot to edit its configuration."));
        editor.setPadding(new Insets(10));
        SplitPane split = new SplitPane(botsList, editor);
        split.setDividerPositions(0.3);

        HBox controls = new HBox(10,
                new Button("Add"), new Button("Edit"), new Button("Duplicate"),
                new Button("Delete"), new Button("Run"), new Button("Stop"));
        controls.setPadding(new Insets(10));

        BorderPane content = new BorderPane();
        content.setCenter(split);
        content.setBottom(controls);

        Tab tab = new Tab("My Bots", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createScriptBuilderTab() {
        ListView<String> palette = new ListView<>(FXCollections.observableArrayList(
                "Tap", "Swipe", "Scroll", "Wait", "Input Text", "If Image",
                "Loop", "OCR Read", "Log", "Exit"));
        palette.setPrefWidth(150);

        ListView<String> steps = new ListView<>(FXCollections.observableArrayList());
        VBox inspector = new VBox(10, new Label("Inspector"), new Label("Select a step to edit properties."));
        inspector.setPadding(new Insets(10));
        VBox variables = new VBox(10, new Label("Variables"), new Label("Define script variables here."));
        variables.setPadding(new Insets(10));

        SplitPane center = new SplitPane(palette, steps, inspector);
        center.setDividerPositions(0.2, 0.6);

        BorderPane content = new BorderPane();
        content.setCenter(center);
        content.setBottom(variables);
        BorderPane.setMargin(variables, new Insets(10));

        Tab tab = new Tab("Script Builder", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createLiveRunnerTab() {
        TableView<RunStatus> table = new TableView<>();
        table.getColumns().add(column("Bot", status -> status.botId));
        table.getColumns().add(column("Instance", status -> status.instanceName));
        table.getColumns().add(column("Script", status -> status.scriptName));
        table.getColumns().add(column("State", status -> status.state));
        table.getColumns().add(column("Message", status -> status.lastMessage));

        ImageView preview = new ImageView();
        preview.setFitWidth(320);
        preview.setFitHeight(180);
        preview.setPreserveRatio(true);

        VBox right = new VBox(10, new Label("Screenshot Preview"), preview,
                new Button("Refresh"), new Button("Stop Selected"), new Button("Stop All"));
        right.setPadding(new Insets(10));

        SplitPane split = new SplitPane(table, right);
        split.setDividerPositions(0.7);

        Tab tab = new Tab("Live Runner", split);
        tab.setClosable(false);
        return tab;
    }

    private Tab createLogsTab() {
        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefRowCount(20);

        ComboBox<String> levelFilter = new ComboBox<>(FXCollections.observableArrayList("DEBUG", "INFO", "WARN", "ERROR"));
        levelFilter.getSelectionModel().select("INFO");

        HBox filters = new HBox(10, new Label("Level:"), levelFilter, new Button("Refresh"));
        filters.setPadding(new Insets(10));

        BorderPane content = new BorderPane();
        content.setTop(filters);
        content.setCenter(logArea);

        Tab tab = new Tab("Logs", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createSettingsTab() {
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField ld5 = new TextField(config.ldplayer5Path);
        TextField ld9 = new TextField(config.ldplayer9Path);
        ComboBox<String> themeBox = new ComboBox<>(FXCollections.observableArrayList("black-blue", "dark-gold"));
        themeBox.getSelectionModel().select(config.theme);
        ComboBox<String> authMode = new ComboBox<>(FXCollections.observableArrayList("local", "cloud"));
        authMode.getSelectionModel().select(config.authMode);

        grid.addRow(0, new Label("LDPlayer 5 Path"), ld5);
        grid.addRow(1, new Label("LDPlayer 9 Path"), ld9);
        grid.addRow(2, new Label("Theme"), themeBox);
        grid.addRow(3, new Label("Auth Mode"), authMode);

        Button save = new Button("Save");
        save.setOnAction(event -> {
            config.ldplayer5Path = ld5.getText();
            config.ldplayer9Path = ld9.getText();
            config.theme = themeBox.getSelectionModel().getSelectedItem();
            config.authMode = authMode.getSelectionModel().getSelectedItem();
            configService.save(config);
            showInfo("Settings saved. Restart to apply authentication changes.");
        });

        VBox box = new VBox(10, grid, save);
        box.setPadding(new Insets(10));

        Tab tab = new Tab("Settings", box);
        tab.setClosable(false);
        return tab;
    }

    private Tab createUpdatesTab() {
        Label version = new Label("Current version: 0.1.0");
        Label manifest = new Label("Updates are currently offline.");
        VBox box = new VBox(10, version, manifest, new Button("Check"));
        box.setPadding(new Insets(10));
        Tab tab = new Tab("Updates", box);
        tab.setClosable(false);
        return tab;
    }

    private TableColumn<RunStatus, String> column(String title, java.util.function.Function<RunStatus, String> mapper) {
        TableColumn<RunStatus, String> column = new TableColumn<>(title);
        column.setCellValueFactory(param -> new javafx.beans.property.SimpleStringProperty(mapper.apply(param.getValue())));
        column.setPrefWidth(120);
        return column;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}