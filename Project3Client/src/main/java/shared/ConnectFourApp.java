package shared;

import Controller.LoginController;
import Controller.RoomView;
import Controller.RoomController;

import java.io.File;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

import javafx.application.Application;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import java.net.SocketTimeoutException;
import javafx.scene.control.ProgressIndicator;
import java.util.Optional;


import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.io.IOException;

public class ConnectFourApp extends Application {
    private Stage primaryStage;
    private User currentUser;
    private Client client;  // or ClientConnection + Client wrapper
    private ClientConnection conn;

    // Font
    public static final String FONT_PATH = "/fonts/m6x11plus.ttf";
    public static final double DEFAULT_FONT_SIZE = 18;
    public static Font globalFont;
    public static String globalFontFamily = "W95FA";

    // Controllers
    private LoginController loginCtrl;
    private shared.RegisterController registerCtrl;
    private shared.ProfileController profileCtrl;

    private Scene roomScene, waitingScene, gameScene, resultScene;
    private String currentRoomId;
    private TextField roomIdField;
    private Label messageLabel;   // ← pull this out
    private TextArea chatArea;
    private RoomController roomCtrl;
    private final List<RoomView> availableRooms = new ArrayList<>();
    private TextArea roomListArea;

    public void setCurrentRoomId(String id) {
        this.currentRoomId = id;
    }

    private void loadCustomFont() {
        globalFont = Font.loadFont(getClass().getResourceAsStream(FONT_PATH), DEFAULT_FONT_SIZE);
        if (globalFont != null) {
            globalFontFamily = globalFont.getFamily();
            System.out.println("Loaded font: " + globalFont.getName());
        } else {
            System.out.println("Failed to load custom font");
        }
    }

    private boolean myTurn;
    public void setMyTurn(boolean isMyTurn) {
        this.myTurn = isMyTurn;
    }

    @Override
    public void start(Stage stage) {
        loadCustomFont();
        this.primaryStage = stage;
        stage.setTitle("Connect Four");

        loginCtrl   = new LoginController(this);
        registerCtrl = new shared.RegisterController(this);
        profileCtrl = new shared.ProfileController(this);

        showLoginScene();
        stage.show();
    }
    public ClientConnection getOrCreateConnection() throws IOException {
        if (conn == null) {
            conn = new ClientConnection();
            conn.connect("localhost", 12345);
        }
        return conn;
    }
    public void finishLogin(String csvPayload) {
        // parse your CSV into currentUser
        String[] parts = csvPayload.split(",", -1);
        currentUser = new User(
                parts[0], parts[1], parts[2],
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                Integer.parseInt(parts[6]),
                Integer.parseInt(parts[7])
        );
        showOptionMenuScene();
    }
    public void logout() {
        // 1) Close the socket, if open
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn = null;

        // 2) Clear any user state
        currentUser = null;
        currentRoomId = null;

        // 3) Go back to the login screen
        showLoginScene();
    }


    public void showLoginScene() {
        primaryStage.setScene(loginCtrl.getScene());
    }

    public void applyGlobalStyles(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/global.css")).toExternalForm());
    }

    public void showRegisterScene() {
        primaryStage.setScene(registerCtrl.getScene());
    }

    public void showOptionMenuScene() {
        Image bgImage = new Image(getClass().getResource("/backgrounds/hi.res.crt.png").toExternalForm());

        BackgroundImage bg = new BackgroundImage(
                bgImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                new BackgroundSize(100, 100, true, true, true, false)
        );

        Label title = new Label("CONNECT FOUR");
//        title.setFont(Font.font(ConnectFourApp.globalFontFamily));  // Big title
        title.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 160;" +
                "-fx-background-color: transparent;" +
                "-fx-padding: 0 0 0 0;" +
                "-fx-text-alignment: center;"
        );
        title.setAlignment(Pos.TOP_CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Button lanButton = new Button("LAN PLAY");
        lanButton.setStyle("-fx-background-color: #0087F1; -fx-text-fill: white; -fx-font-weight: bold;");
        lanButton.setPrefWidth(100);
        Button compButton = new Button("COMPUTER");
        compButton.setPrefWidth(100);
        compButton.setStyle("-fx-background-color: #7D52AE; -fx-text-fill: white; -fx-font-weight: bold;");
        Button howToPlayButton = new Button("HOW TO PLAY");
        howToPlayButton.setPrefWidth(100);
        howToPlayButton.setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-weight: bold;");
        Button profileButton = new Button("PROFILE");
        profileButton.setPrefWidth(100);
        profileButton.setStyle("-fx-background-color: #3E926F; -fx-text-fill: white; -fx-font-weight: bold;");
        Button logoutButton = new Button("LOGOUT");
        logoutButton.setPrefWidth(100);
        logoutButton.setStyle("-fx-background-color: #F24339; -fx-text-fill: white; -fx-font-weight: bold;");

        lanButton.setOnAction(e -> showRoomScene());
        // compBtn.setOnAction(e -> startSinglePlayer());
        howToPlayButton.setOnAction(e -> showHowToPlayScene());
        profileButton.setOnAction(e -> showProfileScene());
        logoutButton.setOnAction(e -> logout());

        VBox root = new VBox(10, lanButton, compButton, howToPlayButton, profileButton, logoutButton);
        root.setStyle(
                "-fx-background-color: #374A4D;" +
                        "-fx-border-width: 3;" +
                        "-fx-border-radius: 10;" +
                        "-fx-background-radius: 10;" +
                        "-fx-effect: dropshadow(gaussian, black, 10, 0.5, 0, 4);"
        );
        root.setAlignment(Pos.CENTER);
        root.setMaxWidth(180);
        root.setMaxHeight(250);
        root.setPrefHeight(300);
        root.setMinHeight(250);

        VBox layout = new VBox(50, title, root);  // 50 px space between title and menu
        layout.setAlignment(Pos.CENTER);
        VBox.setMargin(title, new Insets(0, 0, 0, 0));

        StackPane background = new StackPane(layout); // layout = your VBox with title + buttons
        background.setBackground(new Background(bg));


        Scene scene = new Scene(background, 1600, 900);
        primaryStage.setScene(scene);
        applyGlobalStyles(scene);
    }

    public void showRoomScene() {
        if (roomScene == null) {
            // 1) Create the display area
            TextField roomTitle = new TextField("AVAILABLE ROOMS");
            roomTitle.setFocusTraversable(false);
            roomTitle.setEditable(false);
            roomTitle.setStyle(
                    "-fx-text-fill: white;"+
                    "-fx-font-size: 64;" +
                    "-fx-background-color: #F24339;"+
                    "-fx-border-radius: 10;" +
                    "-fx-background-radius: 10;" +
                    " -fx-border-color: #374A4D;"
            );
            roomTitle.setMinWidth(800);
            roomTitle.setMaxWidth(200);
            roomTitle.setAlignment(Pos.CENTER);

            roomListArea = new TextArea();
            roomListArea.setEditable(false);
            roomListArea.setPrefRowCount(10);
            roomListArea.setWrapText(true);
            roomListArea.setPrefWidth(800);
            roomListArea.setMaxWidth(800);
            roomListArea.setPrefHeight(500);
            roomListArea.setMaxHeight(500);
            roomListArea.setStyle(
                    "-fx-control-inner-background: #1D2529;" +
                    " -fx-border-radius: 10;" +
                    " -fx-background-radius: 10;" +
                    " -fx-border-color: #374A4D;" +
                    "-fx-font-family: '" + ConnectFourApp.globalFontFamily + "';" +
                    "-fx-font-size: 48px;" +
                    "-fx-border-width: 2;" +
                    "-fx-focus-color: transparent;" +
                    "-fx-faint-focus-color: transparent;" +
                    "-fx-text-fill: white;" +
                    "-fx-caret-color: transparent;"
            );
            roomListArea.setEditable(false);
            roomListArea.setFocusTraversable(false);

            // 2) Reuse your existing input controls
            roomIdField = new TextField();
            roomIdField.setPromptText("Enter room ID");

            messageLabel   = new Label();

            Button refreshButton    = new Button("REFRESH");
            refreshButton.setStyle("-fx-background-color: #3E926F; -fx-text-fill: white; -fx-font-weight: bold;");
            Button quickJoinButton  = new Button("QUICK JOIN");
            quickJoinButton.setStyle("-fx-background-color: #0087F1; -fx-text-fill: white; -fx-font-weight: bold;");
            Button joinByIdButton   = new Button("JOIN");
            joinByIdButton.setStyle("-fx-background-color: #FF6368; -fx-text-fill: white; -fx-font-weight: bold;");
            Button createButton     = new Button("CREATE ROOM");
            createButton.setStyle("-fx-background-color: #7D52AE; -fx-text-fill: white; -fx-font-weight: bold;");
            Button backButton       = new Button("BACK");
            backButton.setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-weight: bold;");

            // 3) Instantiate controller with the plain list and text area
            roomCtrl = new RoomController(
                    conn,
                    currentUser,
                    this,
                    availableRooms,
                    roomListArea,
                    roomIdField,
                    messageLabel
            );

            // 4) Wire buttons
            refreshButton.setOnAction(e -> roomCtrl.fetchAvailableRooms());
            quickJoinButton.setOnAction(e -> roomCtrl.handleQuickJoin());
            joinByIdButton.setOnAction(e -> roomCtrl.handleJoinRoomById());
            createButton.setOnAction(e -> roomCtrl.handleCreateRoom());
//            spectateBtn .setOnAction(e -> roomCtrl.handleJoinAsSpectator());
            backButton.setOnAction(e -> showOptionMenuScene());

            HBox idRow = new HBox(8, roomIdField, joinByIdButton);
            idRow.setAlignment(Pos.CENTER_LEFT);
            idRow.setMaxWidth(800);
            HBox buttons = new HBox(10, refreshButton, quickJoinButton, createButton, backButton);
            buttons.setAlignment(Pos.CENTER);

            VBox root = new VBox(10,
                    roomTitle,
                    idRow,
                    roomListArea,
                    messageLabel,
                    buttons
            );
            root.setPadding(new Insets(20));
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #374A4D;");

            roomScene = new Scene(root, 1600, 900);
            applyGlobalStyles(roomScene);
            Platform.runLater(() -> primaryStage.getScene().getRoot().requestFocus());
        }
        else {
            // ** re‑enable and clear them any time you come back **
            roomListArea.setDisable(false);
            roomIdField.setDisable(false);
            roomIdField.clear();
            messageLabel.setText("");
        }

        primaryStage.setScene(roomScene);
        roomCtrl.fetchAvailableRooms();
    }

    public void showWaitingScene() {
        if (waitingScene == null) {
            Label lbl = new Label("Waiting for an opponent to join…");
            lbl.setWrapText(true);
            ProgressIndicator spinner = new ProgressIndicator();

            Button cancel = new Button("Cancel");
            cancel.setOnAction(e -> roomCtrl.cancelCreateRoom());

            VBox root = new VBox(20, spinner, lbl, cancel);
            root.setAlignment(Pos.CENTER);
            root.setPadding(new Insets(30));
            waitingScene = new Scene(root, 1600, 900);
        }
        primaryStage.setScene(waitingScene);
    }




    public void showGameScene() {
        // 1) Build the static board
        GridPane board = new GridPane();
        board.setHgap(5);
        board.setVgap(5);
        board.setPadding(new Insets(10));
        Circle[][] cells = new Circle[6][7];
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                Circle cell = new Circle(20, Color.LIGHTGRAY);
                cell.setStroke(Color.DARKGRAY);
                cells[row][col] = cell;
                board.add(cell, col, row);
            }
        }

        // 2) Turn input UI + status field
        TextField colField = new TextField();
        colField.setPromptText("0–6");
        colField.setPrefWidth(50);

        TextField statusField = new TextField();
        statusField.setEditable(false);
        statusField.setPrefWidth(120);
        statusField.setText(myTurn ? "Your turn!" : "Opponent…");

        Button dropBtn = new Button("Drop");
        dropBtn.setDisable(!myTurn);  // only clickable when it's your turn

        // only enable when a valid 0–6 is entered
        colField.textProperty().addListener((obs, o, n) -> {
            try {
                int c = Integer.parseInt(n.trim());
                dropBtn.setDisable(! (myTurn && c >= 0 && c <= 6));
            } catch (Exception ex) {
                dropBtn.setDisable(true);
            }
        });

        dropBtn.setOnAction(e -> {
            String txt = colField.getText().trim();
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.MOVE,
                        txt,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // immediately switch to opponent:
//            myTurn = false;
//            statusField.setText("Opponent…");
//            dropBtn.setDisable(true);
            colField.clear();
        });

        HBox inputRow = new HBox(10,
                new Label("Column:"), colField, dropBtn, statusField
        );
        inputRow.setAlignment(Pos.CENTER);

        // 3) Chat panel (unchanged)
        TextArea chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        chatArea.setPrefSize(250, 300);

        TextField chatInput = new TextField();
        chatInput.setPromptText("Type message…");
        Button sendBtn = new Button("Send");
        sendBtn.setOnAction(ev -> {
            String txt = chatInput.getText().trim();
            if (!txt.isEmpty()) {
                try {
                    conn.sendMessage(new Message(
                            UUID.randomUUID().toString(),
                            MessageType.CHAT,
                            txt,
                            currentUser.getUsername(),
                            null,
                            System.currentTimeMillis()
                    ));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                chatInput.clear();
            }
        });
        HBox chatForm = new HBox(5, chatInput, sendBtn);
        chatForm.setAlignment(Pos.CENTER);

        Button surrenderBtn = new Button("Surrender");
        surrenderBtn.setOnAction(ev -> {
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.SURRENDER,
                        currentRoomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        VBox chatPane = new VBox(10,
                new Label("Chat"), chatArea, chatForm, surrenderBtn
        );
        chatPane.setPadding(new Insets(10));
        chatPane.setAlignment(Pos.CENTER);

        // 4) Layout everything
        VBox leftPane = new VBox(15, inputRow, board);
        leftPane.setAlignment(Pos.CENTER);
        HBox root = new HBox(20, leftPane, chatPane);
        root.setPadding(new Insets(10));

        gameScene = new Scene(root, 750, 450);
        primaryStage.setScene(gameScene);
        primaryStage.show();

        // 5) Listener thread: paint moves, chat, end, *and* flip turns
        new Thread(() -> {
            try {
                while (true) {
                    Message msg = conn.receiveMessage();
                    MessageType t = msg.getType();
                    if (t == MessageType.ERROR) {
                        // show the alert, then put control back to the user
                        Platform.runLater(() -> {
                            new Alert(AlertType.ERROR, msg.getContent()).showAndWait();
                            // it must still be your turn, so re-enable the drop controls:
                            dropBtn.setDisable(false);
                            statusField.setText("Your turn!");
                        });
                    }

                    else if (t == MessageType.MOVE) {
                        String[] parts = msg.getContent().split(",",2);
                        int c = Integer.parseInt(parts[0]);
                        int r = Integer.parseInt(parts[1]);
                        Color fill = msg.getSender().equals(currentUser.getUsername())
                                ? Color.RED
                                : Color.YELLOW;
                        Platform.runLater(() -> cells[r][c].setFill(fill));

                        // if it was *their* move, now it's your turn
                        if (!msg.getSender().equals(currentUser.getUsername())) {
                            myTurn = true;
                            Platform.runLater(() -> {
                                statusField.setText("Your turn!");
                                dropBtn.setDisable(false);
                            });
                        }
                    }
                    else if (t == MessageType.CHAT) {
                        Platform.runLater(() ->
                                chatArea.appendText(msg.getSender()+": "+msg.getContent()+"\n")
                        );
                    }
                    else if (t == MessageType.GAME_END) {
                        setMyTurn(false);
                        final String outcome;
                        if ("YOU_WIN".equals(msg.getContent())) {
                            outcome = "You won!";
                            currentUser.setScore(currentUser.getScore()+10);
                            currentUser.setWinCount(currentUser.getWinCount() + 1);
                            currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                        }
                        else if ("YOU_LOSE".equals(msg.getContent())) {
                            outcome = "You lost!";
                            if (currentUser.getScore()>0){
                                currentUser.setScore(currentUser.getScore()-10);
                            }
                            currentUser.setLossCount(currentUser.getLossCount() + 1);
                            currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                        }
                        else{
                            outcome = "Draw!";
                            currentUser.setScore(currentUser.getScore());
                            currentUser.setDrawCount(currentUser.getDrawCount() + 1);
                            currentUser.setGamesPlayed(currentUser.getGamesPlayed() + 1);
                        }
                        Platform.runLater(() -> {
                            Platform.runLater(() -> showResultPopUp(outcome));
                            //showOptionMenuScene();
                        });
                        return;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, "GameListener").start();
    }
    private void showResultScene(String outcomeText) {
        Label outcome = new Label(outcomeText);
        outcome.setStyle("-fx-font-size: 36px; -fx-text-fill: #333;");
        outcome.setAlignment(Pos.CENTER);

        Button yes = new Button("Rematch");
        Button no  = new Button("No, back to menu");

        yes.setOnAction(e -> {
            // 1) go into “waiting for opponent” UI
            try{
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.REMATCH_REQUEST,
                        currentRoomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
                System.out.println(currentRoomId+" sended the message rematch to server");
            }
            catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            // 1) immediately show the waiting scene
            showWaitingScene();

            // 2) now spin off the blocking wait into its own thread
            new Thread(() -> {
                try {
                    roomCtrl.waitForGameRematch();   // this blocks until server replies
                    // when it returns, it should have already navigated to gameScene,
                    // or you can handle rejection here:
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // if rematch was rejected (or error), go back on the FX thread:
                    Platform.runLater(() -> showOptionMenuScene());
                }
            }, "Rematch-Wait-Thread").start();
        });

        no.setOnAction(e -> {
            // inform the server to tear down this room:
            try {
                conn.sendMessage(new Message(
                        UUID.randomUUID().toString(),
                        MessageType.REMATCH_REJECT,
                        currentRoomId,
                        currentUser.getUsername(),
                        null,
                        System.currentTimeMillis()
                ));
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
            // then go back to the main menu:
            showOptionMenuScene();
        });

        HBox buttons = new HBox(20, yes, no);
        buttons.setAlignment(Pos.CENTER);

        VBox root = new VBox(40, outcome, buttons);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30));

        resultScene = new Scene(root, 750, 450);
        primaryStage.setScene(resultScene);
    }


    private void showResultPopUp(String outcomeText) {
        // Create an informational Alert dialog
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("End Game!");
        dialog.setHeaderText(null);
        dialog.setContentText(outcomeText);

        // Set custom "Next" button
        ButtonType nextBtn = new ButtonType("Next", ButtonBar.ButtonData.OK_DONE);
        dialog.getButtonTypes().setAll(nextBtn);

        // Create a Timeline to auto-dismiss the popup after 10 seconds
        Timeline autoCloseTimeline = new Timeline(new KeyFrame(Duration.seconds(10), event -> {
            System.out.println("10 seconds passed without user action. Auto-transitioning...");
            dialog.setResult(nextBtn); // Set the result as "Next"
            dialog.hide();             // Close the dialog
        }));
        autoCloseTimeline.setCycleCount(1);
        autoCloseTimeline.play();

        // Show the Alert dialog and wait for the user's response
        Optional<ButtonType> result = dialog.showAndWait();

        // Stop the timer in case the user responded before the timeout
        autoCloseTimeline.stop();

        // Process the user's response (or auto-transition)
        if (result.isPresent() && result.get() == nextBtn) {
            System.out.println("User clicked Next or timed out. Transitioning to result scene...");
            showResultScene(outcomeText);
        }
    }

    private void showHowToPlayScene() {
        TextField title = new TextField("HOW TO PLAY");
        title.setMouseTransparent(true);
        title.setFocusTraversable(false);
        title.setEditable(false);  // Make it read-only
        title.setStyle(
                "-fx-text-fill: white;"+
                "-fx-font-size: 64;" +
                "-fx-background-color: #AE65FF;"+
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                " -fx-border-color: #374A4D;"
        );
        title.setMinWidth(1000);
        title.setMaxWidth(300);
        title.setAlignment(Pos.CENTER);

        HBox titleRow = new HBox(title);
        titleRow.setAlignment(Pos.CENTER);

        TextArea howTo = new TextArea(
                        "- Two players take turns dropping discs into columns.\n\n" +
                        "- Discs fall to the lowest available space in the selected column.\n\n" +
                        "- The goal is to connect four of your discs in a row:\n\n" +
                        "        - Horizontally\n\n" +
                        "        - Vertically\n\n" +
                        "        - Diagonally\n\n" +
                        "- The first player to connect four wins the game.\n\n" +
                        "- If the board is full and no one wins, it’s a draw."
        );
        howTo.setEditable(false);
        howTo.setMouseTransparent(true);
        howTo.setFocusTraversable(false);
        howTo.setPrefWidth(1000);
        howTo.setMaxWidth(1000);
        howTo.setPrefHeight(650);
        howTo.setMaxHeight(650);
        howTo.setStyle(
                "-fx-control-inner-background: #1D2529;" +
                "-fx-background-color: #1D2529;" +
                "-fx-border-color: #374A4D;" +
                "-fx-border-width: 2;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;" +
                "-fx-text-fill: white;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10;" +
                "-fx-caret-color: transparent;" +
                "-fx-font-size: 36px;"
        );

        VBox container = new VBox(howTo);
        container.setAlignment(Pos.CENTER);
        container.setPrefWidth(900);


        Button back = new Button("BACK");
        back.setStyle("-fx-background-color: #F98C02; -fx-text-fill: white; -fx-font-size: 36px");
        back.setPrefWidth(120);
        back.setPrefHeight(40);
        back.setOnAction(e -> showOptionMenuScene());

        HBox buttonBox = new HBox(back);
        buttonBox.setAlignment(Pos.CENTER);

        VBox root = new VBox(10, titleRow, container, buttonBox);
        root.setPadding(new Insets(20));
        primaryStage.setScene(new Scene(root, 1600, 900));
        root.setStyle("-fx-background-color: #374A4D;");
        applyGlobalStyles(primaryStage.getScene());
    }
    private void showProfileScene() {
        profileCtrl.displayProfileInfo(currentUser);
        primaryStage.setScene(profileCtrl.getScene());
    }
    public void showDeleteAccountConfirm() {
        Label confirm = new Label("ARE YOU SURE WANT TO DELETE YOUR ACCOUNT?");
        confirm.setStyle("-fx-font-size: 48px;" +
                "-fx-text-fill: white;" +
                "-fx-background-color: #1D2529"
        );
        confirm.setWrapText(true);
        confirm.setAlignment(Pos.CENTER);
        confirm.setPrefWidth(1400);

        Button yes = new Button("YES");
        yes.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F24339;");
        yes.setPrefWidth(80);
        Button no = new Button("NO");
        no.setStyle("-fx-font-size: 24px; -fx-text-fill: white; -fx-background-color: #F98C02;");
        no.setPrefWidth(80);

        yes.setOnAction(e -> {
            yes.setDisable(true);
            no.setDisable(true);
            confirm.setText("Deleting account...");

            new Thread(this::run, "DeleteAccount-Thread").start();
        });

        no.setOnAction(e -> showProfileScene());

        HBox buttons = new HBox(20, yes, no);
        buttons.setAlignment(Pos.CENTER);
        buttons.setStyle("-fx-background-color: #1D2529");
        VBox innerBox = new VBox(20, confirm, buttons);
        innerBox.setAlignment(Pos.CENTER);
        innerBox.setPrefWidth(1000);
        innerBox.setMaxWidth(1000);
        innerBox.setPadding(new Insets(20));
        innerBox.setStyle(
                "-fx-background-color: #1D2529;" +
                "-fx-border-color: #374A4D;" +
                "-fx-border-radius: 10;" +
                "-fx-background-radius: 10"
        );

        VBox root = new VBox(innerBox);

        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #374A4D;");

        primaryStage.setScene(new Scene(root, 1600, 900));
        applyGlobalStyles(primaryStage.getScene());
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void run() {
        try {
            Message del = new Message(
                    UUID.randomUUID().toString(),
                    MessageType.DELETE_ACCOUNT,
                    currentUser.getPassword(),
                    currentUser.getUsername(),
                    null,
                    System.currentTimeMillis()
            );
            System.out.println("Sending DELETE_ACCOUNT message for user: " + currentUser.getUsername());
            conn.sendMessage(del);

            Message reply = conn.receiveMessage();
            System.out.println("Received response: " + reply.getType() + " - " + reply.getContent());

            Platform.runLater(() -> {
                if (reply.getType() == MessageType.ERROR) {
                    new Alert(AlertType.ERROR, reply.getContent()).showAndWait();
                    showProfileScene();
                } else if (reply.getType() == MessageType.DELETE_ACCOUNT_SUCCESS) {
                    try {
                        conn.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    conn = null;
                    currentUser = null;
                    showLoginScene();
                } else {
                    new Alert(AlertType.ERROR, "Unexpected server response.").showAndWait();
                    showProfileScene();
                }
            });
        } catch (Exception ex) {

        }
    }
}
