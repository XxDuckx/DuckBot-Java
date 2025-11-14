package com.duckbot.app;

import com.duckbot.adb.LdConsoleHelper;
import com.duckbot.core.Config;
import com.duckbot.core.RunStatus;
import com.duckbot.services.*;
import com.duckbot.services.impl.*;
import com.duckbot.store.JsonStore;
import com.duckbot.theme.ThemeManager;
import com.duckbot.util.DataPaths;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Arrays;
import java.util.List;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

/**
 * Entry point for DuckBot JavaFX application.
 */
public final class DuckBotApp extends Application {

    private AuthService authService;
    private ConfigService configService;
    private Config config;
    private com.duckbot.services.BotService botService;
    private com.duckbot.services.RunnerService runnerService;

    @Override
    public void start(Stage stage) {
        DataPaths.root();
        JsonStore store = new JsonStore();
        configService = new FileConfigService(store);
        config = configService.load();
        botService = new com.duckbot.services.impl.FileBotService(store);
        
        // Initialize services
        com.duckbot.services.LogService logService = new com.duckbot.services.impl.FileLogService();
        com.duckbot.services.InstanceRegistry registry = new com.duckbot.services.impl.InMemoryInstanceRegistry();
        runnerService = new com.duckbot.services.impl.DefaultRunnerService(logService, registry);

        AuthProvider provider = createAuthProvider(store, config);
        authService = new AuthService(provider);
        if (provider instanceof LocalAuthProvider localProvider && localProvider.requiresAdminSetup()) {
            promptAdminCreation(localProvider);
        }

        showLogin(stage);
    }

    private AuthProvider createAuthProvider(JsonStore store, Config config) {
        if ("cloud".equalsIgnoreCase(config.authMode)) {
            return new CloudAuthProvider(config.apiBaseUrl);
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
        ThemeManager.apply(scene, "black-blue");
        stage.setTitle("DuckBot Login");
        stage.setScene(scene);
        stage.show();
    }

    private void showMainWindow(Stage stage) {
        Button assetsBtn = new Button("Assets Tool");
        assetsBtn.setOnAction(e -> showAssetsWindow());
        
        TabPane tabs = new TabPane();
        tabs.getTabs().add(createBotsTab());
        tabs.getTabs().add(createScriptBuilderTab());
        tabs.getTabs().add(createLiveRunnerTab());
        tabs.getTabs().add(createLogsTab());
        tabs.getTabs().add(createSettingsTab());
        tabs.getTabs().add(createUpdatesTab());

        ToolBar toolbar = new ToolBar(assetsBtn);
        
        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(tabs);
        Scene scene = new Scene(root, 1000, 700);
        
        // Keyboard shortcuts
        scene.setOnKeyPressed(e -> {
            if (e.isControlDown()) {
                switch (e.getCode()) {
                    case S: // Ctrl+S: Save in current context
                        if (tabs.getSelectionModel().getSelectedIndex() == 1) { // Script Builder
                            showInfo("Ctrl+S: Use Save Script button after validation");
                        }
                        e.consume();
                        break;
                    case R: // Ctrl+R: Refresh/Reload
                        showInfo("Refreshing...");
                        e.consume();
                        break;
                    case Q: // Ctrl+Q: Quit
                        stage.close();
                        e.consume();
                        break;
                    default:
                        break;
                }
            } else if (e.getCode() == javafx.scene.input.KeyCode.F5) { // F5: Refresh
                showInfo("F5: Refresh");
                e.consume();
            }
        });
        
        ThemeManager.apply(scene, "black-blue");
        stage.setTitle("DuckBot Control Center");
        stage.setScene(scene);
        stage.show();
    }

    private void showAssetsWindow() {
        Stage stage = new Stage();
        stage.setTitle("DuckBot Assets Tool");
        stage.initModality(Modality.NONE);
        
        // ADB status indicator
        Label adbStatus = new Label("ADB: Checking...");
        adbStatus.getStyleClass().add("status-label");
        Button testAdb = new Button("Test ADB");
        
        com.duckbot.adb.AdbClient adbClient = new com.duckbot.adb.AdbClient();
        Runnable updateAdbStatus = () -> {
            if (adbClient.testConnection()) {
                adbStatus.setText("ADB: Connected ✓");
                adbStatus.setStyle("-fx-text-fill: lime;");
            } else {
                adbStatus.setText("ADB: Not found ✗");
                adbStatus.setStyle("-fx-text-fill: red;");
            }
        };
        testAdb.setOnAction(e -> updateAdbStatus.run());
        // Initial check
        new Thread(updateAdbStatus).start();

        // Instance selection
        ComboBox<String> instanceBox = new ComboBox<>();
        Button refreshInstances = new Button("Refresh Instances");
        refreshInstances.setOnAction(e -> {
            List<String> roots = Arrays.asList(config.ldplayer5Path, config.ldplayer9Path);
            List<String> names = LdConsoleHelper.detectInstancesFromConsolePaths(roots);
            instanceBox.setItems(FXCollections.observableArrayList(names));
            if (!names.isEmpty()) instanceBox.getSelectionModel().select(0);
        });
        refreshInstances.fire();

        // Game/category/filename
        ComboBox<String> gameBox = new ComboBox<>(FXCollections.observableArrayList("west","al","ants","roe","generic"));
        gameBox.getSelectionModel().select("west");
        ComboBox<String> categoryBox = new ComboBox<>(FXCollections.observableArrayList("game","popups"));
        categoryBox.getSelectionModel().select("game");
        TextField filenameField = new TextField();
        filenameField.setPromptText("file name (without .png)");

        // Screenshot / crop view with zoom/pan
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(520);
        view.setFitHeight(320);
        
        Rectangle selection = new Rectangle(0,0,0,0);
        selection.setStroke(Color.DEEPSKYBLUE);
        selection.setFill(Color.color(0,0,1,0.2));
        selection.setVisible(false);
        
        ScrollPane scrollPane = new ScrollPane();
        StackPane imagePane = new StackPane(view, selection);
        scrollPane.setContent(imagePane);
        scrollPane.setPrefSize(540, 340);
        scrollPane.setFitToWidth(false);
        scrollPane.setFitToHeight(false);
        
        Label info = new Label("Drag to select an area to crop. Scroll to zoom.");
        
        // Zoom controls
        Slider zoomSlider = new Slider(0.5, 3.0, 1.0);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setMajorTickUnit(0.5);
        zoomSlider.setPrefWidth(200);
        Label zoomLabel = new Label("Zoom: 100%");
        zoomSlider.valueProperty().addListener((obs, old, val) -> {
            double scale = val.doubleValue();
            view.setScaleX(scale);
            view.setScaleY(scale);
            zoomLabel.setText(String.format("Zoom: %.0f%%", scale * 100));
        });
        
        Button resetZoom = new Button("Reset");
        resetZoom.setOnAction(e -> zoomSlider.setValue(1.0));
        
        HBox zoomControls = new HBox(8, new Label("Zoom:"), zoomSlider, zoomLabel, resetZoom);
        zoomControls.setPadding(new Insets(4));

        final BufferedImage[] lastShot = new BufferedImage[1];
        final double[] start = new double[2];

        imagePane.setOnMousePressed(ev -> {
            if (view.getImage() == null) return;
            start[0] = ev.getX();
            start[1] = ev.getY();
            selection.setX(start[0]);
            selection.setY(start[1]);
            selection.setWidth(0);
            selection.setHeight(0);
            selection.setVisible(true);
        });
        imagePane.setOnMouseDragged(ev -> {
            if (!selection.isVisible()) return;
            double x = Math.min(start[0], ev.getX());
            double y = Math.min(start[1], ev.getY());
            double w = Math.abs(ev.getX() - start[0]);
            double h = Math.abs(ev.getY() - start[1]);
            selection.setX(x);
            selection.setY(y);
            selection.setWidth(w);
            selection.setHeight(h);
            info.setText(String.format("Selection: %.0fx%.0f", w, h));
        });

        Button captureBtn = new Button("Capture Screenshot");
        captureBtn.setOnAction(e -> {
            String inst = instanceBox.getSelectionModel().getSelectedItem();
            if (inst == null || inst.isBlank()) { showError("Select an instance to capture from"); return; }
            try {
                // Resolve instance name to ADB serial
                List<String> roots = Arrays.asList(config.ldplayer5Path, config.ldplayer9Path);
                String serial = com.duckbot.adb.AdbClient.resolveInstanceSerial(inst, roots);
                
                com.duckbot.adb.AdbClient adb = new com.duckbot.adb.AdbClient();
                BufferedImage img = adb.screencap(serial);
                lastShot[0] = img;
                Image fx = SwingFXUtils.toFXImage(img, null);
                view.setImage(fx);
                selection.setVisible(false);
                String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
                filenameField.setText("crop_" + ts);
                info.setText("Screenshot captured from " + serial);
            } catch (Exception ex) {
                showError("Failed to capture: " + ex.getMessage());
            }
        });

        Button saveBtn = new Button("Save Crop");
        saveBtn.setOnAction(e -> {
            if (lastShot[0] == null) { showError("Capture a screenshot first"); return; }
            if (!selection.isVisible() || selection.getWidth() < 2 || selection.getHeight() < 2) { showError("Select an area to crop"); return; }
            String game = gameBox.getSelectionModel().getSelectedItem();
            String cat = categoryBox.getSelectionModel().getSelectedItem();
            String fname = filenameField.getText();
            if (fname == null || fname.isBlank()) { showError("Enter a file name"); return; }
            try {
                Image fxImg = view.getImage();
                double dispW = view.getBoundsInLocal().getWidth();
                double dispH = view.getBoundsInLocal().getHeight();
                double scaleX = fxImg.getWidth() / dispW;
                double scaleY = fxImg.getHeight() / dispH;
                int x = (int)Math.round(selection.getX() * scaleX);
                int y = (int)Math.round(selection.getY() * scaleY);
                int w = (int)Math.round(selection.getWidth() * scaleX);
                int h = (int)Math.round(selection.getHeight() * scaleY);
                // clamp
                x = Math.max(0, Math.min(x, lastShot[0].getWidth()-1));
                y = Math.max(0, Math.min(y, lastShot[0].getHeight()-1));
                w = Math.max(1, Math.min(w, lastShot[0].getWidth()-x));
                h = Math.max(1, Math.min(h, lastShot[0].getHeight()-y));
                BufferedImage sub = lastShot[0].getSubimage(x, y, w, h);
                Path out = Paths.get("data","images", game, cat, fname + ".png");
                Files.createDirectories(out.getParent());
                ImageIO.write(sub, "PNG", out.toFile());
                showInfo("Saved " + out);
            } catch (Exception ex) {
                showError("Failed to save: " + ex.getMessage());
            }
        });

        HBox top = new HBox(10, adbStatus, testAdb, new Label("Instance:"), instanceBox, refreshInstances,
                new Label("Game:"), gameBox, new Label("Category:"), categoryBox,
                new Label("Name:"), filenameField, captureBtn, saveBtn);
        top.setPadding(new Insets(8));

        VBox content = new VBox(8, top, scrollPane, zoomControls, info);
        content.setPadding(new Insets(10));
        
        Scene scene = new Scene(content, 900, 600);
        ThemeManager.apply(scene, "black-blue");
        stage.setScene(scene);
        stage.show();
    }

    private void showCoordinatePicker(Spinner<Integer> targetSpinner, String coordLabel) {
        Stage picker = new Stage();
        picker.setTitle("Pick Coordinate - " + coordLabel);
        picker.initModality(Modality.APPLICATION_MODAL);
        
        Label info = new Label("1. Select instance and capture screenshot\n2. Click on the image to set coordinate");
        info.setWrapText(true);
        
        // Instance selector
        ComboBox<String> instanceBox = new ComboBox<>();
        Button refreshBtn = new Button("Refresh");
        refreshBtn.setOnAction(e -> {
            List<String> roots = Arrays.asList(config.ldplayer5Path, config.ldplayer9Path);
            List<String> names = LdConsoleHelper.detectInstancesFromConsolePaths(roots);
            instanceBox.setItems(FXCollections.observableArrayList(names));
            if (!names.isEmpty()) instanceBox.getSelectionModel().select(0);
        });
        refreshBtn.fire();
        
        Button captureBtn = new Button("Capture Screenshot");
        HBox topBar = new HBox(10, new Label("Instance:"), instanceBox, refreshBtn, captureBtn);
        topBar.setPadding(new Insets(8));
        
        ImageView imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(540);
        imageView.setFitHeight(960);
        
        Label coordLabel2 = new Label("Click on image to select coordinate");
        coordLabel2.setStyle("-fx-font-weight: bold;");
        
        ScrollPane scroll = new ScrollPane(imageView);
        scroll.setPrefSize(560, 500);
        
        final BufferedImage[] screenshot = new BufferedImage[1];
        
        imageView.setOnMouseClicked(ev -> {
            if (screenshot[0] == null) return;
            double imgW = imageView.getBoundsInLocal().getWidth();
            double imgH = imageView.getBoundsInLocal().getHeight();
            double scaleX = screenshot[0].getWidth() / imgW;
            double scaleY = screenshot[0].getHeight() / imgH;
            int x = (int)Math.round(ev.getX() * scaleX);
            int y = (int)Math.round(ev.getY() * scaleY);
            targetSpinner.getValueFactory().setValue(coordLabel.toLowerCase().contains("x") ? x : y);
            coordLabel2.setText(String.format("Selected: %d (Click OK to apply)", coordLabel.toLowerCase().contains("x") ? x : y));
        });
        
        captureBtn.setOnAction(e -> {
            String inst = instanceBox.getValue();
            if (inst == null || inst.isBlank()) {
                showError("Select an instance first");
                return;
            }
            try {
                List<String> roots = Arrays.asList(config.ldplayer5Path, config.ldplayer9Path);
                String serial = com.duckbot.adb.AdbClient.resolveInstanceSerial(inst, roots);
                com.duckbot.adb.AdbClient adb = new com.duckbot.adb.AdbClient();
                screenshot[0] = adb.screencap(serial);
                Image fx = SwingFXUtils.toFXImage(screenshot[0], null);
                imageView.setImage(fx);
                coordLabel2.setText("Click on image to select coordinate");
            } catch (Exception ex) {
                showError("Failed to capture: " + ex.getMessage());
            }
        });
        
        Button okBtn = new Button("OK");
        okBtn.setOnAction(e -> picker.close());
        Button cancelBtn = new Button("Cancel");
        cancelBtn.setOnAction(e -> picker.close());
        HBox btnBar = new HBox(10, okBtn, cancelBtn);
        btnBar.setPadding(new Insets(8));
        btnBar.setAlignment(javafx.geometry.Pos.CENTER);
        
        VBox content = new VBox(10, info, topBar, scroll, coordLabel2, btnBar);
        content.setPadding(new Insets(10));
        
        Scene scene = new Scene(content, 600, 700);
        ThemeManager.apply(scene, "black-blue");
        picker.setScene(scene);
        picker.showAndWait();
    }

    private Tab createBotsTab() {
        // Load bot profiles from file
        ObservableList<com.duckbot.core.BotProfile> bots = FXCollections.observableArrayList(botService.loadAll());

        ListView<com.duckbot.core.BotProfile> list = new ListView<>(bots);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(com.duckbot.core.BotProfile bot, boolean empty) {
                super.updateItem(bot, empty);
                if (empty || bot == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                CheckBox select = new CheckBox();
                Label id = new Label(String.valueOf(getIndex()));
                TextField name = new TextField(bot.name != null ? bot.name : bot.id);
                name.setPrefWidth(380);
                name.textProperty().addListener((obs, old, val) -> bot.name = val);
                Button edit = new Button("✎");
                edit.setOnAction(e -> showBotSettings());
                Button run = new Button("▶");
                run.setOnAction(e -> {
                    String runId = runnerService.start(bot);
                    showInfo("Started bot " + bot.name + " with runId: " + runId);
                });
                Button pause = new Button("■");
                pause.setOnAction(e -> {
                    runnerService.stop(bot.id);
                    showInfo("Stopped bot " + bot.name);
                });
                Button step = new Button("▷");
                Button settings = new Button("⚙");
                settings.setOnAction(e -> showBotSettings());
                Button cam = new Button("📷");
                cam.setOnAction(e -> showAssetsWindow());
                Button view = new Button("👁");
                view.setOnAction(e -> showInfo("Live view feature - coming in future update"));
                Button del = new Button("🗑");
                del.setOnAction(e -> {
                    botService.delete(bot);
                    bots.remove(bot);
                });
                for (Button b : new Button[]{edit, run, pause, step, settings, cam, view, del}) {
                    b.setMinWidth(34);
                }

                HBox row = new HBox(12, select, id, name, run, pause, step, settings, cam, view, del);
                row.setPadding(new Insets(8, 10, 8, 10));
                setGraphic(row);
            }
        });

        // Bottom action bar
        CheckBox selectAll = new CheckBox("Select all");
        Button startSelected = new Button("Start selected");
        startSelected.setOnAction(e -> {
            long count = bots.stream().filter(b -> b.name != null).count();
            showInfo("Started " + count + " selected bots");
        });
        Button stopSelected = new Button("Stop selected");
        stopSelected.setOnAction(e -> {
            bots.forEach(b -> runnerService.stop(b.id));
            showInfo("Stopped all selected bots");
        });
        Button deleteSelected = new Button("Delete selected");
        deleteSelected.setOnAction(e -> showInfo("Delete selected - feature coming soon"));
        Button quickEditor = new Button("Quick editor");
        quickEditor.setOnAction(e -> showInfo("Quick editor - feature coming soon"));
        Button fixEmulators = new Button("Fix emulators");
        fixEmulators.setOnAction(e -> showInfo("Fix emulators - feature coming soon"));
        Button createInstance = new Button("Create bot instance");
        createInstance.setOnAction(e -> {
            com.duckbot.core.BotProfile newBot = new com.duckbot.core.BotProfile();
            newBot.name = "New Bot";
            newBot.game = "west";
            botService.save(newBot);
            bots.add(newBot);
        });
        HBox bottom = new HBox(12, selectAll, startSelected, stopSelected, deleteSelected, quickEditor, fixEmulators, createInstance);
        bottom.setPadding(new Insets(10));

        BorderPane content = new BorderPane();
        content.setCenter(list);
        content.setBottom(bottom);

        Tab tab = new Tab("My Bots", content);
        tab.setClosable(false);
        return tab;
    }

    private Tab createScriptBuilderTab() {
        // === MODELS ===
        ObservableList<com.duckbot.scripts.ScriptVariable> varData = FXCollections.observableArrayList();
        ObservableList<com.duckbot.scripts.EditableStep> stepData = FXCollections.observableArrayList();
        ObservableList<String> stepNames = FXCollections.observableArrayList();
        
        // === TOP BAR: Metadata + Validation ===
        TextField scriptNameField = new TextField();
        scriptNameField.setPromptText("Script Name");
        scriptNameField.setPrefWidth(180);
        TextField gameField = new TextField();
        gameField.setPromptText("Game");
        gameField.setPrefWidth(100);
        TextField authorField = new TextField();
        authorField.setPromptText("Author");
        authorField.setPrefWidth(130);
        Button saveBtn = new Button("Save");
        Button loadBtn = new Button("Load");
        Button testBtn = new Button("Test");
        
        HBox topBar = new HBox(10, new Label("Script:"), scriptNameField, new Label("Game:"), gameField, 
                                new Label("Author:"), authorField, saveBtn, loadBtn, testBtn);
        topBar.setPadding(new Insets(8));
        
        ListView<String> errorList = new ListView<>();
        errorList.setPrefHeight(50);
        errorList.setPlaceholder(new Label("✓ Ready to save"));
        
        VBox topSection = new VBox(4, topBar, errorList);
        
        // === LEFT COLUMN: Variables ===
        Label varLabel = new Label("VARIABLES");
        varLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        TableView<com.duckbot.scripts.ScriptVariable> varTable = new TableView<>(varData);
        varTable.setPrefHeight(280);
        TableColumn<com.duckbot.scripts.ScriptVariable, String> vKeyCol = new TableColumn<>("Key");
        vKeyCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().key));
        vKeyCol.setPrefWidth(90);
        TableColumn<com.duckbot.scripts.ScriptVariable, String> vTypeCol = new TableColumn<>("Type");
        vTypeCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().type));
        vTypeCol.setPrefWidth(70);
        TableColumn<com.duckbot.scripts.ScriptVariable, String> vPromptCol = new TableColumn<>("Prompt");
        vPromptCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().prompt));
        vPromptCol.setPrefWidth(150);
        varTable.getColumns().addAll(vKeyCol, vTypeCol, vPromptCol);
        
        // Variable Editor Form
        GridPane varEditor = new GridPane();
        varEditor.setHgap(8);
        varEditor.setVgap(6);
        varEditor.setPadding(new Insets(8));
        TextField vKey = new TextField();
        vKey.setPromptText("key");
        TextField vPrompt = new TextField();
        vPrompt.setPromptText("prompt");
        ComboBox<String> vType = new ComboBox<>(FXCollections.observableArrayList("text","number","boolean","select","multiselect","weekdays"));
        vType.setValue("text");
        TextField vDefault = new TextField();
        vDefault.setPromptText("default");
        TextField vOptions = new TextField();
        vOptions.setPromptText("option1,option2");
        TextField vMin = new TextField();
        vMin.setPromptText("min");
        vMin.setPrefWidth(60);
        TextField vMax = new TextField();
        vMax.setPromptText("max");
        vMax.setPrefWidth(60);
        TextField vSection = new TextField();
        vSection.setPromptText("section");
        Button vAdd = new Button("Add/Update");
        Button vDel = new Button("Delete");
        
        int vr = 0;
        varEditor.addRow(vr++, new Label("Key:"), vKey);
        varEditor.addRow(vr++, new Label("Prompt:"), vPrompt);
        varEditor.addRow(vr++, new Label("Type:"), vType);
        varEditor.addRow(vr++, new Label("Default:"), vDefault);
        varEditor.addRow(vr++, new Label("Options:"), vOptions);
        HBox minMax = new HBox(6, vMin, vMax);
        varEditor.addRow(vr++, new Label("Min/Max:"), minMax);
        varEditor.addRow(vr++, new Label("Section:"), vSection);
        HBox vBtns = new HBox(8, vAdd, vDel);
        varEditor.add(vBtns, 1, vr);
        
        VBox leftCol = new VBox(8, varLabel, varTable, varEditor);
        leftCol.setPadding(new Insets(10));
        leftCol.setPrefWidth(380);
        
        // === RIGHT COLUMN: Steps ===
        Label stepLabel = new Label("STEPS");
        stepLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        ListView<String> stepList = new ListView<>(stepNames);
        stepList.setPrefHeight(280);
        
        ComboBox<String> stepTypeBox = new ComboBox<>(FXCollections.observableArrayList(
            com.duckbot.scripts.StepFactory.getSupportedStepTypes()));
        stepTypeBox.setPromptText("Select step type...");
        stepTypeBox.setPrefWidth(180);
        Button addStepBtn = new Button("Add Step");
        Button delStepBtn = new Button("Delete");
        Button moveUpBtn = new Button("↑");
        Button moveDownBtn = new Button("↓");
        
        // Step templates/presets
        MenuButton templatesBtn = new MenuButton("📋 Templates");
        templatesBtn.setStyle("-fx-font-size: 11px;");
        MenuItem tmplWaitTap = new MenuItem("Wait + Tap");
        MenuItem tmplTapTapTap = new MenuItem("Triple Tap");
        MenuItem tmplSwipeUp = new MenuItem("Swipe Up (Scroll)");
        MenuItem tmplClosePopup = new MenuItem("Close Popup (if image)");
        MenuItem tmplCollectAll = new MenuItem("Collect Loop");
        templatesBtn.getItems().addAll(tmplWaitTap, tmplTapTapTap, tmplSwipeUp, tmplClosePopup, tmplCollectAll);
        
        HBox stepControls = new HBox(8, stepTypeBox, addStepBtn, delStepBtn, moveUpBtn, moveDownBtn, templatesBtn);
        
        // Dynamic property editor (visual forms)
        Label stepPropLabel = new Label("Step Properties:");
        GridPane stepPropForm = new GridPane();
        stepPropForm.setHgap(8);
        stepPropForm.setVgap(6);
        stepPropForm.setPadding(new Insets(8));
        stepPropForm.setPrefHeight(250);
        ScrollPane stepPropScroll = new ScrollPane(stepPropForm);
        stepPropScroll.setFitToWidth(true);
        stepPropScroll.setPrefHeight(260);
        
        VBox rightCol = new VBox(8, stepLabel, stepList, stepControls, stepPropLabel, stepPropScroll);
        rightCol.setPadding(new Insets(10));
        rightCol.setPrefWidth(450);
        
        // === LAYOUT ===
        HBox mainContent = new HBox(12, leftCol, rightCol);
        BorderPane root = new BorderPane();
        root.setTop(topSection);
        root.setCenter(mainContent);
        
        // === VARIABLE LOGIC ===
        varTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            vKey.setText(sel.key);
            vPrompt.setText(sel.prompt);
            vType.setValue(sel.type != null ? sel.type : "text");
            vDefault.setText(sel.defaultValue != null ? String.valueOf(sel.defaultValue) : "");
            vOptions.setText(sel.options != null && !sel.options.isEmpty() ? String.join(",", sel.options.stream().map(Object::toString).toList()) : "");
            vMin.setText(sel.min != null ? sel.min.toString() : "");
            vMax.setText(sel.max != null ? sel.max.toString() : "");
            vSection.setText(sel.section != null ? sel.section : "");
        });
        
        vAdd.setOnAction(e -> {
            String key = vKey.getText();
            if (key == null || key.isBlank()) {
                showError("Variable key required");
                return;
            }
            com.duckbot.scripts.ScriptVariable var = varData.stream().filter(v -> v.key.equals(key)).findFirst().orElse(null);
            if (var == null) {
                var = new com.duckbot.scripts.ScriptVariable();
                varData.add(var);
            }
            var.key = key;
            var.prompt = vPrompt.getText();
            var.type = vType.getValue();
            var.defaultValue = parseDefault(var.type, vDefault.getText());
            var.options.clear();
            if (("select".equals(var.type) || "multiselect".equals(var.type)) && !vOptions.getText().isBlank()) {
                for (String opt : vOptions.getText().split(",")) var.options.add(opt.trim());
            }
            var.min = parseDouble(vMin.getText());
            var.max = parseDouble(vMax.getText());
            var.section = vSection.getText();
            varTable.refresh();
            validateAndShowErrors(errorList, saveBtn, varData, scriptNameField.getText(), gameField.getText());
        });
        
        vDel.setOnAction(e -> {
            varData.removeIf(v -> v.key.equals(vKey.getText()));
            validateAndShowErrors(errorList, saveBtn, varData, scriptNameField.getText(), gameField.getText());
        });
        
        // === STEP LOGIC ===
        addStepBtn.setOnAction(e -> {
            String type = stepTypeBox.getValue();
            if (type == null || type.isBlank()) {
                showError("Select a step type first");
                return;
            }
            com.duckbot.scripts.EditableStep es = new com.duckbot.scripts.EditableStep();
            es.type = type;
            // Default properties
            switch (type.toLowerCase()) {
                case "tap" -> {
                    es.props.put("x", "100");
                    es.props.put("y", "200");
                    es.props.put("delay", "500");
                }
                case "swipe" -> {
                    es.props.put("x1", "300");
                    es.props.put("y1", "800");
                    es.props.put("x2", "300");
                    es.props.put("y2", "200");
                    es.props.put("durationMs", "300");
                }
                case "scroll" -> {
                    es.props.put("direction", "DOWN");
                    es.props.put("distance", "400");
                    es.props.put("durationMs", "300");
                }
                case "wait" -> {
                    es.props.put("delay", "1000");
                }
                case "input text" -> {
                    es.props.put("text", "Sample text");
                }
                case "if image" -> {
                    es.props.put("imagePath", "data/images/game/popup.png");
                    es.props.put("confidence", "0.9");
                }
                case "loop" -> {
                    es.props.put("count", "5");
                }
                case "ocr read" -> {
                    es.props.put("region", "0,0,100,50");
                    es.props.put("outVar", "ocrResult");
                }
                case "log" -> {
                    es.props.put("message", "Log message here");
                }
                case "custom js" -> {
                    es.props.put("code", "// JavaScript code");
                }
            }
            stepData.add(es);
            stepNames.add(type + " (#" + stepData.size() + ")");
        });
        
        delStepBtn.setOnAction(e -> {
            int idx = stepList.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                stepData.remove(idx);
                stepNames.remove(idx);
            }
        });
        
        moveUpBtn.setOnAction(e -> {
            int idx = stepList.getSelectionModel().getSelectedIndex();
            if (idx > 0) {
                stepData.add(idx - 1, stepData.remove(idx));
                stepNames.add(idx - 1, stepNames.remove(idx));
                stepList.getSelectionModel().select(idx - 1);
            }
        });
        
        moveDownBtn.setOnAction(e -> {
            int idx = stepList.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < stepData.size() - 1) {
                stepData.add(idx + 1, stepData.remove(idx));
                stepNames.add(idx + 1, stepNames.remove(idx));
                stepList.getSelectionModel().select(idx + 1);
            }
        });
        
        // Template actions
        tmplWaitTap.setOnAction(e -> {
            addStepFromTemplate(stepData, stepNames, "wait", "delay", "2000");
            addStepFromTemplate(stepData, stepNames, "tap", "x", "500", "y", "800", "delay", "500");
            showInfo("Added: Wait 2s + Tap");
        });
        
        tmplTapTapTap.setOnAction(e -> {
            addStepFromTemplate(stepData, stepNames, "tap", "x", "500", "y", "800", "delay", "500");
            addStepFromTemplate(stepData, stepNames, "wait", "delay", "500");
            addStepFromTemplate(stepData, stepNames, "tap", "x", "500", "y", "800", "delay", "500");
            addStepFromTemplate(stepData, stepNames, "wait", "delay", "500");
            addStepFromTemplate(stepData, stepNames, "tap", "x", "500", "y", "800", "delay", "500");
            showInfo("Added: Triple Tap sequence");
        });
        
        tmplSwipeUp.setOnAction(e -> {
            addStepFromTemplate(stepData, stepNames, "swipe", "x1", "300", "y1", "1200", "x2", "300", "y2", "400", "durationMs", "300");
            showInfo("Added: Swipe Up");
        });
        
        tmplClosePopup.setOnAction(e -> {
            addStepFromTemplate(stepData, stepNames, "if image", "imagePath", "data/images/generic/popups/close_btn.png", "confidence", "0.85", "timeout", "2000");
            addStepFromTemplate(stepData, stepNames, "tap", "x", "950", "y", "150", "delay", "500");
            showInfo("Added: Close Popup (if image + tap)");
        });
        
        tmplCollectAll.setOnAction(e -> {
            addStepFromTemplate(stepData, stepNames, "loop", "count", "5");
            addStepFromTemplate(stepData, stepNames, "tap", "x", "540", "y", "960", "delay", "500");
            addStepFromTemplate(stepData, stepNames, "wait", "delay", "1000");
            showInfo("Added: Collect Loop (5x tap + wait)");
        });
        
        // Dynamic form population based on selected step
        stepList.getSelectionModel().selectedIndexProperty().addListener((o, old, idx) -> {
            stepPropForm.getChildren().clear();
            if (idx == null || idx.intValue() < 0 || idx.intValue() >= stepData.size()) return;
            
            com.duckbot.scripts.EditableStep es = stepData.get(idx.intValue());
            int row = 0;
            
            // Build form based on step type
            switch (es.type.toLowerCase()) {
                case "tap" -> {
                    row = addCoordinateField(stepPropForm, row, "X:", es, "x", 0, 2000, 10, stepList, idx);
                    row = addCoordinateField(stepPropForm, row, "Y:", es, "y", 0, 3000, 10, stepList, idx);
                    row = addSpinnerField(stepPropForm, row, "Delay (ms):", es, "delay", 0, 10000, 100);
                }
                case "swipe" -> {
                    row = addCoordinateField(stepPropForm, row, "X1:", es, "x1", 0, 2000, 10, stepList, idx);
                    row = addCoordinateField(stepPropForm, row, "Y1:", es, "y1", 0, 3000, 10, stepList, idx);
                    row = addCoordinateField(stepPropForm, row, "X2:", es, "x2", 0, 2000, 10, stepList, idx);
                    row = addCoordinateField(stepPropForm, row, "Y2:", es, "y2", 0, 3000, 10, stepList, idx);
                    row = addSpinnerField(stepPropForm, row, "Duration (ms):", es, "durationMs", 50, 5000, 50);
                }
                case "scroll" -> {
                    row = addComboField(stepPropForm, row, "Direction:", es, "direction", 
                        new String[]{"UP", "DOWN", "LEFT", "RIGHT"});
                    row = addSpinnerField(stepPropForm, row, "Distance:", es, "distance", 50, 2000, 50);
                    row = addSpinnerField(stepPropForm, row, "Duration (ms):", es, "durationMs", 50, 5000, 50);
                }
                case "wait" -> {
                    row = addSpinnerField(stepPropForm, row, "Delay (ms):", es, "delay", 100, 60000, 100);
                }
                case "input text" -> {
                    row = addTextField(stepPropForm, row, "Text:", es, "text");
                }
                case "if image" -> {
                    row = addTextField(stepPropForm, row, "Image Path:", es, "imagePath");
                    Button browseImg = new Button("Browse...");
                    browseImg.setOnAction(ev -> {
                        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                        fc.setInitialDirectory(new java.io.File("data/images"));
                        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PNG Images", "*.png"));
                        java.io.File f = fc.showOpenDialog(null);
                        if (f != null) {
                            es.props.put("imagePath", f.getPath().replace("\\", "/"));
                            stepList.refresh();
                            stepList.getSelectionModel().select(idx.intValue()); // Refresh form
                        }
                    });
                    stepPropForm.add(browseImg, 2, row - 1);
                    row = addSpinnerField(stepPropForm, row, "Confidence:", es, "confidence", 0.1, 1.0, 0.05);
                    row = addSpinnerField(stepPropForm, row, "Timeout (ms):", es, "timeout", 1000, 30000, 1000);
                }
                case "loop" -> {
                    row = addSpinnerField(stepPropForm, row, "Count:", es, "count", 1, 1000, 1);
                }
                case "ocr read" -> {
                    row = addTextField(stepPropForm, row, "Region (x,y,w,h):", es, "region");
                    row = addTextField(stepPropForm, row, "Output Variable:", es, "outVar");
                }
                case "log" -> {
                    row = addTextField(stepPropForm, row, "Message:", es, "message");
                }
                case "custom js" -> {
                    Object codeVal = es.props.get("code");
                    TextArea codeArea = new TextArea(codeVal != null ? String.valueOf(codeVal) : "");
                    codeArea.setPrefRowCount(10);
                    codeArea.setWrapText(true);
                    codeArea.textProperty().addListener((ob, o2, n2) -> es.props.put("code", n2));
                    stepPropForm.add(new Label("JavaScript Code:"), 0, row);
                    stepPropForm.add(codeArea, 1, row, 2, 1);
                }
                case "exit" -> {
                    Label info = new Label("(No properties - exits script immediately)");
                    info.setStyle("-fx-text-fill: gray;");
                    stepPropForm.add(info, 0, row, 3, 1);
                }
                default -> {
                    // Generic key=value editor for unknown types
                    TextArea genericArea = new TextArea();
                    genericArea.setPromptText("key=value (one per line)");
                    genericArea.setPrefRowCount(8);
                    StringBuilder sb = new StringBuilder();
                    es.props.forEach((k, v) -> sb.append(k).append("=").append(v).append("\n"));
                    genericArea.setText(sb.toString());
                    genericArea.textProperty().addListener((ob, o2, val) -> {
                        es.props.clear();
                        for (String line : val.split("\n")) {
                            if (line.isBlank()) continue;
                            int eq = line.indexOf('=');
                            if (eq < 0) continue;
                            es.props.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                        }
                    });
                    stepPropForm.add(new Label("Properties:"), 0, row);
                    stepPropForm.add(genericArea, 1, row, 2, 1);
                }
            }
        });
        
        // === FILE OPERATIONS ===
        scriptNameField.textProperty().addListener((o,old,v) -> validateAndShowErrors(errorList, saveBtn, varData, scriptNameField.getText(), gameField.getText()));
        gameField.textProperty().addListener((o,old,v) -> validateAndShowErrors(errorList, saveBtn, varData, scriptNameField.getText(), gameField.getText()));
        
        saveBtn.setOnAction(e -> saveScriptToJson(varData, stepData, scriptNameField.getText(), gameField.getText(), authorField.getText()));
        loadBtn.setOnAction(e -> loadScriptFromJson(varData, stepData, stepNames, scriptNameField, gameField, authorField, null, errorList, saveBtn));
        testBtn.setOnAction(e -> testScriptInBuilder(varData, stepData, scriptNameField.getText(), gameField.getText()));
        
        // Drag and drop JSON files to load
        root.setOnDragOver(ev -> {
            if (ev.getDragboard().hasFiles()) {
                ev.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            ev.consume();
        });
        root.setOnDragDropped(ev -> {
            var db = ev.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                for (java.io.File file : db.getFiles()) {
                    if (file.getName().endsWith(".json")) {
                        try {
                            String json = java.nio.file.Files.readString(file.toPath());
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            com.duckbot.scripts.EditableScript script = gson.fromJson(json, com.duckbot.scripts.EditableScript.class);
                            varData.clear();
                            if (script.variables != null) varData.addAll(script.variables);
                            stepData.clear();
                            stepNames.clear();
                            if (script.steps != null) {
                                for (int i=0;i<script.steps.size();i++) {
                                    com.duckbot.scripts.EditableStep es = script.steps.get(i);
                                    stepData.add(es);
                                    stepNames.add(es.type + " (#" + (i+1) + ")");
                                }
                            }
                            scriptNameField.setText(script.name);
                            gameField.setText(script.game);
                            authorField.setText(script.author);
                            validateAndShowErrors(errorList, saveBtn, varData, script.name, script.game);
                            showInfo("Loaded " + file.getName());
                            success = true;
                            break;
                        } catch (Exception ignored) {}
                    }
                }
            }
            ev.setDropCompleted(success);
            ev.consume();
        });
        
        validateAndShowErrors(errorList, saveBtn, varData, scriptNameField.getText(), gameField.getText());
        
        Tab tab = new Tab("Script Builder", root);
        tab.setClosable(false);
        return tab;
    }

    // --- Script Builder helpers ---
    
    // Helper: Add step from template with varargs properties
    private void addStepFromTemplate(ObservableList<com.duckbot.scripts.EditableStep> stepData, 
                                      ObservableList<String> stepNames, String type, String... props) {
        com.duckbot.scripts.EditableStep es = new com.duckbot.scripts.EditableStep();
        es.type = type;
        for (int i = 0; i < props.length; i += 2) {
            if (i + 1 < props.length) {
                es.props.put(props[i], props[i + 1]);
            }
        }
        stepData.add(es);
        stepNames.add(type + " (#" + stepData.size() + ")");
    }
    
    // Helper: Add coordinate field with "Pick" button for visual selection
    private int addCoordinateField(GridPane form, int row, String label, com.duckbot.scripts.EditableStep step, String key, 
                                     int min, int max, int stepVal, ListView<String> stepList, Number selectedIdx) {
        String val = step.props.get(key) != null ? String.valueOf(step.props.get(key)) : null;
        Spinner<Integer> spinner = new Spinner<>(min, max, parseIntProp(val, min), stepVal);
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        spinner.valueProperty().addListener((obs, old, newVal) -> step.props.put(key, String.valueOf(newVal)));
        
        Button pickBtn = new Button("📍 Pick");
        pickBtn.setStyle("-fx-font-size: 10px;");
        pickBtn.setOnAction(e -> showCoordinatePicker(spinner, key));
        
        form.add(new Label(label), 0, row);
        HBox coordBox = new HBox(6, spinner, pickBtn);
        form.add(coordBox, 1, row);
        return row + 1;
    }
    
    // Helper: Add spinner field for integer values
    private int addSpinnerField(GridPane form, int row, String label, com.duckbot.scripts.EditableStep step, String key, int min, int max, int stepVal) {
        String val = step.props.get(key) != null ? String.valueOf(step.props.get(key)) : null;
        Spinner<Integer> spinner = new Spinner<>(min, max, parseIntProp(val, min), stepVal);
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        spinner.valueProperty().addListener((obs, old, newVal) -> step.props.put(key, String.valueOf(newVal)));
        form.add(new Label(label), 0, row);
        form.add(spinner, 1, row);
        return row + 1;
    }
    
    // Helper: Add spinner field for double values
    private int addSpinnerField(GridPane form, int row, String label, com.duckbot.scripts.EditableStep step, String key, double min, double max, double stepVal) {
        String val = step.props.get(key) != null ? String.valueOf(step.props.get(key)) : null;
        Spinner<Double> spinner = new Spinner<>(min, max, parseDoubleProp(val, min), stepVal);
        spinner.setEditable(true);
        spinner.setPrefWidth(120);
        spinner.valueProperty().addListener((obs, old, newVal) -> step.props.put(key, String.valueOf(newVal)));
        form.add(new Label(label), 0, row);
        form.add(spinner, 1, row);
        return row + 1;
    }
    
    // Helper: Add text field
    private int addTextField(GridPane form, int row, String label, com.duckbot.scripts.EditableStep step, String key) {
        Object val = step.props.get(key);
        TextField field = new TextField(val != null ? String.valueOf(val) : "");
        field.setPrefWidth(250);
        field.textProperty().addListener((obs, old, newVal) -> step.props.put(key, newVal));
        form.add(new Label(label), 0, row);
        form.add(field, 1, row, 2, 1);
        return row + 1;
    }
    
    // Helper: Add combo box field
    private int addComboField(GridPane form, int row, String label, com.duckbot.scripts.EditableStep step, String key, String[] options) {
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(options));
        Object val = step.props.get(key);
        combo.setValue(val != null ? String.valueOf(val) : options[0]);
        combo.setPrefWidth(150);
        combo.valueProperty().addListener((obs, old, newVal) -> step.props.put(key, newVal));
        form.add(new Label(label), 0, row);
        form.add(combo, 1, row);
        return row + 1;
    }
    
    private int parseIntProp(String val, int defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try { return Integer.parseInt(val); } catch (Exception e) { return defaultVal; }
    }
    
    private double parseDoubleProp(String val, double defaultVal) {
        if (val == null || val.isBlank()) return defaultVal;
        try { return Double.parseDouble(val); } catch (Exception e) { return defaultVal; }
    }
    
    private Object parseDefault(String type, String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return switch (type) {
                case "number" -> Double.parseDouble(raw.trim());
                case "boolean" -> Boolean.parseBoolean(raw.trim());
                default -> raw;
            };
        } catch (Exception e) {
            return raw;
        }
    }

    private Double parseDouble(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private void refreshPreview(VBox previewBox, ObservableList<com.duckbot.scripts.ScriptVariable> vars, String name, String game, String author) {
        if (previewBox == null) return;
        previewBox.getChildren().clear();
        if (name == null || name.isBlank()) name = "(unnamed)";
        com.duckbot.scripts.Script script = new com.duckbot.scripts.Script();
        script.name = name;
        script.game = game;
        script.author = author;
        script.variables = new java.util.ArrayList<>(vars);
        ScriptFormRenderer.FormRef form = ScriptFormRenderer.build(script);
        previewBox.getChildren().addAll(new Label(script.name + " preview"), form.node);
    }

    private void saveScriptToJson(ObservableList<com.duckbot.scripts.ScriptVariable> vars, ObservableList<com.duckbot.scripts.EditableStep> steps, String name, String game, String author) {
        if (name == null || name.isBlank() || game == null || game.isBlank()) { showError("Script name and game required"); return; }
        com.duckbot.scripts.EditableScript script = new com.duckbot.scripts.EditableScript();
        script.name = name; script.game = game; script.author = author;
        script.variables = new java.util.ArrayList<>(vars);
        script.steps = new java.util.ArrayList<>(steps);
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        java.nio.file.Path path = java.nio.file.Paths.get("data","scripts", game, name + ".json");
        try {
            java.nio.file.Files.createDirectories(path.getParent());
            java.nio.file.Files.writeString(path, gson.toJson(script));
            showInfo("Saved to " + path);
        } catch (Exception e) {
            showError("Failed to save: " + e.getMessage());
        }
    }

    private void loadScriptFromJson(ObservableList<com.duckbot.scripts.ScriptVariable> vars, ObservableList<com.duckbot.scripts.EditableStep> steps, ObservableList<String> stepNames, TextField nameField, TextField gameField, TextField authorField, VBox previewBox, ListView<String> errorList, Button saveButton) {
        // Basic file chooser alternative: load by name/game if file exists
        String name = nameField.getText(); String game = gameField.getText();
        if (name == null || name.isBlank() || game == null || game.isBlank()) { showError("Enter script name and game to load"); return; }
        java.nio.file.Path path = java.nio.file.Paths.get("data","scripts", game, name + ".json");
        if (!java.nio.file.Files.exists(path)) { showError("File not found: " + path); return; }
        try {
            String json = java.nio.file.Files.readString(path);
            com.google.gson.Gson gson = new com.google.gson.Gson();
            com.duckbot.scripts.EditableScript script = gson.fromJson(json, com.duckbot.scripts.EditableScript.class);
            vars.clear();
            if (script.variables != null) vars.addAll(script.variables);
            steps.clear(); stepNames.clear();
            if (script.steps != null) {
                for (int i=0;i<script.steps.size();i++) {
                    com.duckbot.scripts.EditableStep es = script.steps.get(i);
                    steps.add(es);
                    stepNames.add(es.type + " (#" + (i+1) + ")");
                }
            }
            nameField.setText(script.name);
            gameField.setText(script.game);
            authorField.setText(script.author);
            refreshPreview(previewBox, vars, script.name, script.game, script.author);
            validateAndShowErrors(errorList, saveButton, vars, script.name, script.game);
            showInfo("Loaded " + path);
        } catch (Exception e) {
            showError("Failed to load: " + e.getMessage());
        }
    }

    private void validateAndShowErrors(ListView<String> errorList, Button saveButton, List<com.duckbot.scripts.ScriptVariable> vars, String name, String game) {
        java.util.List<String> errors = new java.util.ArrayList<>();
        if (name == null || name.isBlank()) errors.add("Script name required");
        if (game == null || game.isBlank()) errors.add("Game id required");
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (com.duckbot.scripts.ScriptVariable v : vars) {
            if (v.key == null || v.key.isBlank()) { errors.add("Variable with empty key"); continue; }
            if (!seen.add(v.key)) errors.add("Duplicate key: " + v.key);
            String type = v.type;
            if (type == null || type.isBlank()) errors.add("Type missing for key: " + v.key);
            else if (!java.util.Set.of("text","number","boolean","select","multiselect","weekdays").contains(type)) errors.add("Unknown type: " + type + " (" + v.key + ")");
            if (("select".equals(type) || "multiselect".equals(type)) && (v.options == null || v.options.isEmpty())) errors.add("Options required for " + type + " variable: " + v.key);
            if ("number".equals(type)) {
                if (v.min != null && v.max != null && v.min > v.max) errors.add("Min greater than Max for " + v.key);
                if (v.defaultValue instanceof Number n) {
                    double dv = n.doubleValue();
                    if (v.min != null && dv < v.min) errors.add("Default below min for " + v.key);
                    if (v.max != null && dv > v.max) errors.add("Default above max for " + v.key);
                }
            }
        }
        errorList.getItems().setAll(errors);
        saveButton.setDisable(!errors.isEmpty());
    }

    private void testScriptInBuilder(ObservableList<com.duckbot.scripts.ScriptVariable> vars, ObservableList<com.duckbot.scripts.EditableStep> steps, String name, String game) {
        if (name == null || name.isBlank() || game == null || game.isBlank()) { showError("Script name and game required for test"); return; }
        
        // Convert EditableScript to runtime Script
        com.duckbot.scripts.EditableScript editable = new com.duckbot.scripts.EditableScript();
        editable.name = name;
        editable.game = game;
        editable.variables = new java.util.ArrayList<>(vars);
        editable.steps = new java.util.ArrayList<>(steps);
        
        com.duckbot.scripts.Script runtime = com.duckbot.scripts.StepFactory.toRuntimeScript(editable);
        
        // Show summary dialog
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Script Test");
        alert.setHeaderText("Dry-run validation for: " + name);
        StringBuilder summary = new StringBuilder();
        summary.append("Game: ").append(game).append("\n");
        summary.append("Variables: ").append(vars.size()).append("\n");
        summary.append("Steps: ").append(steps.size()).append("\n\n");
        summary.append("Converted runtime steps:\n");
        for (int i = 0; i < runtime.steps.size(); i++) {
            com.duckbot.scripts.Step s = runtime.steps.get(i);
            summary.append((i+1)).append(". ").append(s.type()).append("\n");
        }
        alert.setContentText(summary.toString());
        alert.showAndWait();
    }

    private Tab createLiveRunnerTab() {
        TableView<RunStatus> table = new TableView<>();
        ObservableList<RunStatus> runData = FXCollections.observableArrayList();
        table.setItems(runData);
        table.getColumns().add(column("Bot", status -> status.botId));
        table.getColumns().add(column("Instance", status -> status.instanceName));
        table.getColumns().add(column("Script", status -> status.scriptName));
        table.getColumns().add(column("State", status -> status.state));
        table.getColumns().add(column("Message", status -> status.lastMessage));

        ImageView preview = new ImageView();
        preview.setFitWidth(320);
        preview.setFitHeight(180);
        preview.setPreserveRatio(true);
        
        Button refreshBtn = new Button("Refresh");
        Button stopSelectedBtn = new Button("Stop Selected");
        Button stopAllBtn = new Button("Stop All");
        
        // Polling timer to refresh run status
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(new javafx.animation.KeyFrame(
            javafx.util.Duration.seconds(2),
            e -> {
                runData.setAll(runnerService.list());
            }
        ));
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
        
        refreshBtn.setOnAction(e -> runData.setAll(runnerService.list()));
        
        stopSelectedBtn.setOnAction(e -> {
            RunStatus selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) runnerService.stop(selected.runId);
        });
        
        stopAllBtn.setOnAction(e -> {
            for (RunStatus status : runData) {
                runnerService.stop(status.runId);
            }
        });
        
        // Capture screenshot of selected instance
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            try {
                List<String> roots = Arrays.asList(config.ldplayer5Path, config.ldplayer9Path);
                String serial = com.duckbot.adb.AdbClient.resolveInstanceSerial(sel.instanceName, roots);
                com.duckbot.adb.AdbClient adb = new com.duckbot.adb.AdbClient();
                BufferedImage img = adb.screencap(serial);
                Image fx = SwingFXUtils.toFXImage(img, null);
                preview.setImage(fx);
            } catch (Exception ignored) {}
        });

        VBox right = new VBox(10, new Label("Screenshot Preview"), preview,
                refreshBtn, stopSelectedBtn, stopAllBtn);
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
        grid.setPadding(new Insets(12));
        grid.setHgap(10);
        grid.setVgap(8);

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
        Button botSettings = new Button("Open Bot Settings...");
        save.setOnAction(event -> {
            config.ldplayer5Path = ld5.getText();
            config.ldplayer9Path = ld9.getText();
            config.theme = themeBox.getSelectionModel().getSelectedItem();
            config.authMode = authMode.getSelectionModel().getSelectedItem();
            configService.save(config);
            showInfo("Settings saved. Restart to apply authentication changes.");
        });
        botSettings.setOnAction(e -> showBotSettings());

        HBox actions = new HBox(10, save, botSettings);

        VBox box = new VBox(12, grid, actions);
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

    // --- Bot Settings dialog (LSS-style skeleton) ---
    private void showBotSettings() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Bot Settings");

        TabPane tabPane = new TabPane();

        // Scripts tab (loads scripts and renders dynamic form)
        VBox scriptsBox = buildScriptsTabContent();
        Tab scriptsTab = new Tab("Scripts", scriptsBox);
        scriptsTab.setClosable(false);

        // Emulators tab with LDPlayer detection
        ListView<String> instancesList = new ListView<>();
        instancesList.setPrefHeight(240);
        Button refreshBtn = new Button("Refresh Emulator List");
        refreshBtn.setOnAction(e -> {
            List<String> roots = Arrays.asList(config.ldplayer5Path, config.ldplayer9Path);
            List<String> names = LdConsoleHelper.detectInstancesFromConsolePaths(roots);
            ObservableList<String> items = FXCollections.observableArrayList(names);
            if (items.isEmpty()) {
                items.add("No instances detected. Check LDPlayer paths.");
            }
            instancesList.setItems(items);
        });
        HBox createBox = new HBox(8, new Label("Create"), new Spinner<Integer>(1, 20, 1), new Label("new emulator instances"), new Button("Create"));
        VBox emuBox = new VBox(10, new Label("Manage Emulators"), instancesList, refreshBtn, createBox);
        emuBox.setPadding(new Insets(12));
        Tab emulatorsTab = new Tab("Emulators", emuBox);
        emulatorsTab.setClosable(false);

        // Other Settings tab (skeleton controls)
        GridPane otherGrid = new GridPane();
        otherGrid.setHgap(10);
        otherGrid.setVgap(8);
        otherGrid.setPadding(new Insets(12));
        CheckBox stopAfterLoop = new CheckBox("Stop bot instance and close emulators after a full loop");
        otherGrid.add(stopAfterLoop, 0, 0);
        Tab otherTab = new Tab("Other Settings", otherGrid);
        otherTab.setClosable(false);

        // Mail Login / Account Manager tab
        TableView<com.duckbot.core.AccountProfile> accountTable = new TableView<>();
        accountTable.setPrefHeight(300);
        
        TableColumn<com.duckbot.core.AccountProfile, String> accUserCol = new TableColumn<>("Username");
        accUserCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().username));
        accUserCol.setPrefWidth(120);
        
        TableColumn<com.duckbot.core.AccountProfile, String> accEmailCol = new TableColumn<>("Email");
        accEmailCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().email));
        accEmailCol.setPrefWidth(200);
        
        TableColumn<com.duckbot.core.AccountProfile, String> accPinCol = new TableColumn<>("PIN");
        accPinCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().pin));
        accPinCol.setPrefWidth(80);
        
        TableColumn<com.duckbot.core.AccountProfile, String> accActiveCol = new TableColumn<>("Active");
        accActiveCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().active ? "✓" : "✗"));
        accActiveCol.setPrefWidth(60);
        
        accountTable.getColumns().addAll(accUserCol, accEmailCol, accPinCol, accActiveCol);
        
        ObservableList<com.duckbot.core.AccountProfile> accounts = FXCollections.observableArrayList();
        accountTable.setItems(accounts);
        
        // Account editor form
        GridPane accForm = new GridPane();
        accForm.setHgap(10); accForm.setVgap(8); accForm.setPadding(new Insets(10));
        
        TextField accUser = new TextField(); accUser.setPromptText("username");
        TextField accEmail = new TextField(); accEmail.setPromptText("email@example.com");
        TextField accPin = new TextField(); accPin.setPromptText("PIN/password");
        CheckBox accActive = new CheckBox("Active");
        accActive.setSelected(true);
        
        Button addAcc = new Button("Add Account");
        Button delAcc = new Button("Delete");
        Button loadAccs = new Button("Load from File");
        Button saveAccs = new Button("Save to File");
        
        int ar = 0;
        accForm.addRow(ar++, new Label("Username:"), accUser);
        accForm.addRow(ar++, new Label("Email:"), accEmail);
        accForm.addRow(ar++, new Label("PIN:"), accPin);
        accForm.addRow(ar++, new Label("Status:"), accActive);
        HBox accBtns = new HBox(10, addAcc, delAcc);
        accForm.add(accBtns, 1, ar);
        
        accountTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel == null) return;
            accUser.setText(sel.username);
            accEmail.setText(sel.email);
            accPin.setText(sel.pin);
            accActive.setSelected(sel.active);
        });
        
        addAcc.setOnAction(e -> {
            String user = accUser.getText();
            if (user == null || user.isBlank()) { showError("Username required"); return; }
            com.duckbot.core.AccountProfile acc = accounts.stream()
                .filter(a -> user.equals(a.username)).findFirst().orElse(null);
            if (acc == null) {
                acc = new com.duckbot.core.AccountProfile();
                accounts.add(acc);
            }
            acc.username = user;
            acc.email = accEmail.getText();
            acc.pin = accPin.getText();
            acc.active = accActive.isSelected();
            accountTable.refresh();
        });
        
        delAcc.setOnAction(e -> {
            com.duckbot.core.AccountProfile sel = accountTable.getSelectionModel().getSelectedItem();
            if (sel != null) accounts.remove(sel);
        });
        
        loadAccs.setOnAction(e -> {
            java.nio.file.Path path = java.nio.file.Paths.get("data", "accounts.json");
            if (!java.nio.file.Files.exists(path)) { showError("No accounts.json found"); return; }
            try {
                String json = java.nio.file.Files.readString(path);
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.duckbot.core.AccountProfile[] arr = gson.fromJson(json, com.duckbot.core.AccountProfile[].class);
                accounts.clear();
                if (arr != null) accounts.addAll(java.util.Arrays.asList(arr));
                showInfo("Loaded " + accounts.size() + " accounts");
            } catch (Exception ex) {
                showError("Failed to load: " + ex.getMessage());
            }
        });
        
        saveAccs.setOnAction(e -> {
            java.nio.file.Path path = java.nio.file.Paths.get("data", "accounts.json");
            try {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
                java.nio.file.Files.createDirectories(path.getParent());
                java.nio.file.Files.writeString(path, gson.toJson(accounts));
                showInfo("Saved " + accounts.size() + " accounts");
            } catch (Exception ex) {
                showError("Failed to save: " + ex.getMessage());
            }
        });
        
        HBox accTopBar = new HBox(10, loadAccs, saveAccs);
        accTopBar.setPadding(new Insets(8));
        
        VBox mailBox = new VBox(10, accTopBar, accountTable, new Label("Account Editor:"), accForm);
        mailBox.setPadding(new Insets(12));
        Tab mailTab = new Tab("Mail Login", mailBox);
        mailTab.setClosable(false);

        tabPane.getTabs().addAll(scriptsTab, emulatorsTab, otherTab, mailTab);

        BorderPane root = new BorderPane(tabPane);
        root.setPadding(new Insets(8));
        Scene scene = new Scene(root, 900, 620);
        ThemeManager.apply(scene, "black-blue");
        dialog.setScene(scene);
        dialog.show();
    }

    private VBox buildScriptsTabContent() {
        // Game selector and script list
        ComboBox<String> gameBox = new ComboBox<>(FXCollections.observableArrayList(
                "west", "al", "ants", "roe", "generic"));
        gameBox.getSelectionModel().select("west");

        ListView<com.duckbot.games.GameScriptManager.GameScriptFile> scriptsList = new ListView<>();
        scriptsList.setPrefWidth(250);

        VBox rightPane = new VBox(10);
        rightPane.setPadding(new Insets(10));

        // Load scripts when game changes
        com.duckbot.games.GameScriptManager gsm = new com.duckbot.games.GameScriptManager(java.nio.file.Paths.get("data"));
        java.util.function.Consumer<String> loadScripts = gid -> {
            com.duckbot.games.GameScriptManager.GameScripts gs = gsm.getGameScripts(gid);
            scriptsList.setItems(FXCollections.observableArrayList(gs.getScripts()));
        };
        gameBox.setOnAction(e -> loadScripts.accept(gameBox.getSelectionModel().getSelectedItem()));
        loadScripts.accept("west");

        // On script selection, render dynamic form with preset controls
        scriptsList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            rightPane.getChildren().clear();
            if (sel == null) return;
            try {
                String json = sel.getContent();
                com.google.gson.Gson gson = new com.google.gson.Gson();
                com.duckbot.scripts.Script script = gson.fromJson(json, com.duckbot.scripts.Script.class);
                ScriptFormRenderer.FormRef form = ScriptFormRenderer.build(script);
                Label title = new Label(script.name + " by " + (script.author != null ? script.author : "unknown"));

                // Preset area
                TextField presetName = new TextField();
                presetName.setPromptText("Enter a file name here");
                Button saveToFile = new Button("Save To File");
                Button applyToAll = new Button("Apply To All");

                ComboBox<String> existing = new ComboBox<>();
                Button loadToMenu = new Button("Load To Menu");

                com.duckbot.store.ScriptPresetStore presetStore = new com.duckbot.store.ScriptPresetStore(java.nio.file.Paths.get("data"));
                Runnable refreshPresets = () -> {
                    var names = presetStore.listPresets(gameBox.getSelectionModel().getSelectedItem(), sel.getName());
                    existing.setItems(FXCollections.observableArrayList(names));
                };
                refreshPresets.run();

                saveToFile.setOnAction(ev -> {
                    String name = presetName.getText();
                    if (name == null || name.isBlank()) { new Alert(Alert.AlertType.WARNING, "Enter preset name", ButtonType.OK).showAndWait(); return; }
                    try {
                        var values = ScriptFormRenderer.collectValues(script, form.controls);
                        presetStore.savePreset(gameBox.getSelectionModel().getSelectedItem(), sel.getName(), name, values);
                        refreshPresets.run();
                        new Alert(Alert.AlertType.INFORMATION, "Preset saved", ButtonType.OK).showAndWait();
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Failed to save: " + ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                });

                loadToMenu.setOnAction(ev -> {
                    String name = existing.getSelectionModel().getSelectedItem();
                    if (name == null) return;
                    try {
                        var values = presetStore.loadPreset(gameBox.getSelectionModel().getSelectedItem(), sel.getName(), name);
                        ScriptFormRenderer.applyValues(form.controls, values);
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Failed to load: " + ex.getMessage(), ButtonType.OK).showAndWait();
                    }
                });

                HBox saveRow = new HBox(10, presetName, saveToFile, applyToAll);
                HBox loadRow = new HBox(10, existing, loadToMenu);

                rightPane.getChildren().addAll(title, form.node, saveRow, loadRow);
            } catch (Exception ex) {
                rightPane.getChildren().add(new Label("Failed to load script: " + ex.getMessage()));
            }
        });

        SplitPane split = new SplitPane(new VBox(6, new Label("Game:"), gameBox, new Label("Scripts"), scriptsList), rightPane);
        split.setDividerPositions(0.33);

        VBox box = new VBox(10, split);
        box.setPadding(new Insets(12));
        return box;
    }
}